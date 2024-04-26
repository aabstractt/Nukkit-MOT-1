package cn.nukkit.nbt.snbt;

import java.io.IOException;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;

import static cn.nukkit.nbt.snbt.SNBTConstants.TokenType.*;
import static java.nio.charset.StandardCharsets.UTF_8;


public class SNBTLexer implements SNBTConstants {
    static final private SNBTNfaData.NfaFunction[] nfaFunctions = SNBTNfaData.getFunctionTableMap(null);
    static final int DEFAULT_TAB_SIZE = 1;
    private int tabSize = DEFAULT_TAB_SIZE;

    /**
     * set the tab size used for location reporting
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    final Token DUMMY_START_TOKEN = new Token();
    // Just a dummy Token value that we put in the tokenLocationTable
    // to indicate that this location in the file is ignored.
    static final private Token IGNORED = new Token(), SKIPPED = new Token();

    static {
        IGNORED.setUnparsed(true);
        SKIPPED.setUnparsed(true);
    }

    // Munged content, possibly replace unicode escapes, tabs, or CRLF with LF.
    private CharSequence content;
    // Typically a filename, I suppose.
    private String inputSource = "input";
    // A list of offsets of the beginning of lines
    private int[] lineOffsets;
    // The starting line and column, usually 1,1
    // that is used to report a file position 
    // in 1-based line/column terms
    private int startingLine, startingColumn;
    // The offset in the internal buffer to the very
    // next character that the readChar method returns
    private int bufferPosition;
    // A BitSet that stores where the tokens are located.
    // This is not strictly necessary, I suppose...
    private BitSet tokenOffsets;
    //  A Bitset that stores the line numbers that
    // contain either hard tabs or extended (beyond 0xFFFF) unicode
    // characters.
    private BitSet needToCalculateColumns = new BitSet();
    // Just a very simple, bloody minded approach, just store the
    // Token objects in a table where the offsets are the code unit 
    // positions in the content buffer. If the Token at a given offset is
    // the dummy or marker type IGNORED, then the location is skipped via
    // whatever preprocessor logic.    
    private Token[] tokenLocationTable;
    // The following two BitSets are used to store 
    // the current active NFA states in the core tokenization loop
    private BitSet nextStates = new BitSet(76), currentStates = new BitSet(76);
    EnumSet<TokenType> activeTokenTypes = EnumSet.allOf(TokenType.class);

    {
    }

    // Token types that are "regular" tokens that participate in parsing,
    // i.e. declared as TOKEN
    static final EnumSet<TokenType> regularTokens = EnumSet.of(EOF, COLON, COMMA, OPEN_BRACKET, CLOSE_BRACKET, OPEN_BRACE, CLOSE_BRACE, BOOLEAN, FLOAT, DOUBLE, INTEGER, LONG, BYTE, SHORT, STRING, B, _TOKEN_17, I);
    // Token types that do not participate in parsing, a.k.a. "special" tokens in legacy JavaCC,
    // i.e. declared as UNPARSED (or SPECIAL_TOKEN)
    static private final EnumSet<TokenType> unparsedTokens = EnumSet.noneOf(TokenType.class);
    // Tokens that are skipped, i.e. SKIP 
    static final EnumSet<TokenType> skippedTokens = EnumSet.of(WHITESPACE);
    // Tokens that correspond to a MORE, i.e. that are pending 
    // additional input
    static private final EnumSet<TokenType> moreTokens = EnumSet.noneOf(TokenType.class);

    // The source of the raw characters that we are scanning  
    public String getInputSource() {
        return inputSource;
    }

    public void setInputSource(String inputSource) {
        this.inputSource = inputSource;
    }

    public SNBTLexer(CharSequence input) {
        this("input", input);
    }

    /**
     * @param inputSource just the name of the input source (typically the filename)
     *                    that will be used in error messages and so on.
     * @param input       the input
     */
    public SNBTLexer(String inputSource, CharSequence input) {
        this(inputSource, input, LexicalState.SNBT, 1, 1);
    }

    /**
     * @param inputSource just the name of the input source (typically the filename) that
     *                    will be used in error messages and so on.
     * @param input       the input
     * @param lexState    the lexical state to use
     * @param startingLine
     * The line number at which we are starting for the purposes of location/error messages.
     * In most normal usage, this is 1.
     * @param startingColumn
     * The column number at which we are starting for the purposes of location/error messages.
     * In most normal usages this is 1.
     */
    public SNBTLexer(String inputSource, CharSequence input, LexicalState lexState, int startingLine, int startingColumn) {
        this.inputSource = inputSource;
        this.content = mungeContent(input, true, false, false, false);
        this.inputSource = inputSource;
        createLineOffsetsTable();
        tokenLocationTable = new Token[content.length() + 1];
        tokenOffsets = new BitSet(content.length() + 1);
        this.startingLine = startingLine;
        this.startingColumn = startingColumn;
        switchTo(lexState);
    }

    /**
     * Preferably use the constructor that takes a #java.nio.files.Path or simply a String,
     * depending on your use case
     */
    @Deprecated
    public SNBTLexer(Reader reader) {
        this("input", reader, LexicalState.SNBT, 1, 1);
    }

    /**
     * Preferably use the constructor that takes a #java.nio.files.Path or simply a String,
     * depending on your use case
     */
    @Deprecated
    public SNBTLexer(String inputSource, Reader reader) {
        this(inputSource, reader, LexicalState.SNBT, 1, 1);
    }

    /**
     * Preferably use the constructor that takes a #java.nio.files.Path or simply a String,
     * depending on your use case
     */
    @Deprecated
    public SNBTLexer(String inputSource, Reader reader, LexicalState lexState, int line, int column) {
        this(inputSource, readToEnd(reader), lexState, line, column);
        switchTo(lexState);
    }

    private Token getNextToken() {
        InvalidToken invalidToken = null;
        Token token = nextToken();
        while (token instanceof InvalidToken) {
            if (invalidToken == null) {
                invalidToken = (InvalidToken) token;
            } else {
                invalidToken.setEndOffset(token.getEndOffset());
            }
            token = nextToken();
        }
        if (invalidToken != null)
            cacheToken(invalidToken);
        cacheToken(token);
        if (invalidToken != null) {
            goTo(invalidToken.getEndOffset());
            return invalidToken;
        }
        return token;
    }

    /**
     * The public method for getting the next token.
     * If the tok parameter is null, it just tokenizes
     * starting at the internal bufferPosition
     * Otherwise, it checks whether we have already cached
     * the token after this one. If not, it finally goes
     * to the NFA machinery
     */
    public Token getNextToken(Token tok) {
        if (tok == null) {
            return getNextToken();
        }
        Token cachedToken = tok.nextCachedToken();
        // If the cached next token is not currently active, we
        // throw it away and go back to the XXXLexer
        if (cachedToken != null && !activeTokenTypes.contains(cachedToken.getType())) {
            reset(tok);
            cachedToken = null;
        }
        return cachedToken != null ? cachedToken : getNextToken(tok.getEndOffset());
    }

    /**
     * A lower level method to tokenize, that takes the absolute
     * offset into the content buffer as a parameter
     *
     * @param offset where to start
     * @return the token that results from scanning from the given starting point
     */
    public Token getNextToken(int offset) {
        goTo(offset);
        return getNextToken();
    }

    // The main method to invoke the NFA machinery
    private final Token nextToken() {
        Token matchedToken = null;
        boolean inMore = false;
        int tokenBeginOffset = this.bufferPosition, firstChar = 0;
        // The core tokenization loop
        while (matchedToken == null) {
            int curChar, codeUnitsRead = 0, matchedPos = 0;
            TokenType matchedType = null;
            boolean reachedEnd = false;
            if (inMore) {
                curChar = readChar();
                if (curChar == -1)
                    reachedEnd = true;
            } else {
                tokenBeginOffset = this.bufferPosition;
                firstChar = curChar = readChar();
                if (curChar == -1) {
                    matchedType = TokenType.EOF;
                    reachedEnd = true;
                }
            }
            // the core NFA loop
            if (!reachedEnd) do {
                // Holder for the new type (if any) matched on this iteration
                TokenType newType = null;
                if (codeUnitsRead > 0) {
                    // What was nextStates on the last iteration 
                    // is now the currentStates!
                    BitSet temp = currentStates;
                    currentStates = nextStates;
                    nextStates = temp;
                    int retval = readChar();
                    if (retval >= 0) {
                        curChar = retval;
                    } else {
                        reachedEnd = true;
                        break;
                    }
                }
                nextStates.clear();
                int nextActive = codeUnitsRead == 0 ? 0 : currentStates.nextSetBit(0);
                do {
                    TokenType returnedType = nfaFunctions[nextActive].apply(curChar, nextStates, activeTokenTypes);
                    if (returnedType != null && (newType == null || returnedType.ordinal() < newType.ordinal())) {
                        newType = returnedType;
                    }
                    nextActive = codeUnitsRead == 0 ? -1 : currentStates.nextSetBit(nextActive + 1);
                }
                while (nextActive != -1);
                ++codeUnitsRead;
                if (curChar > 0xFFFF)
                    ++codeUnitsRead;
                if (newType != null) {
                    matchedType = newType;
                    inMore = moreTokens.contains(matchedType);
                    matchedPos = codeUnitsRead;
                }
            }
            while (!nextStates.isEmpty());
            if (matchedType == null) {
                bufferPosition = tokenBeginOffset + 1;
                if (firstChar > 0xFFFF)
                    ++bufferPosition;
                return new InvalidToken(this, tokenBeginOffset, bufferPosition);
            }
            bufferPosition -= (codeUnitsRead - matchedPos);
            if (skippedTokens.contains(matchedType)) {
                for (int i = tokenBeginOffset; i < bufferPosition; i++) {
                    if (tokenLocationTable[i] != IGNORED)
                        tokenLocationTable[i] = SKIPPED;
                }
            } else if (regularTokens.contains(matchedType) || unparsedTokens.contains(matchedType)) {
                matchedToken = Token.newToken(matchedType, this, tokenBeginOffset, bufferPosition);
                matchedToken.setUnparsed(!regularTokens.contains(matchedType));
            }
        }
        return matchedToken;
    }

    LexicalState lexicalState = LexicalState.values()[0];

    /**
     * Switch to specified lexical state.
     *
     * @param lexState the lexical state to switch to
     * @return whether we switched (i.e. we weren't already in the desired lexical state)
     */
    public boolean switchTo(LexicalState lexState) {
        if (this.lexicalState != lexState) {
            this.lexicalState = lexState;
            return true;
        }
        return false;
    }

    // Reset the token source input
    // to just after the Token passed in.
    void reset(Token t, LexicalState state) {
        goTo(t.getEndOffset());
        uncacheTokens(t);
        if (state != null) {
            switchTo(state);
        }
    }

    void reset(Token t) {
        reset(t, null);
    }

    // But there is no goto in Java!!!
    private void goTo(int offset) {
        while (offset < content.length() && tokenLocationTable[offset] == IGNORED) {
            ++offset;
        }
        this.bufferPosition = offset;
    }

    private int readChar() {
        while (tokenLocationTable[bufferPosition] == IGNORED && bufferPosition < content.length()) {
            ++bufferPosition;
        }
        if (bufferPosition >= content.length()) {
            return -1;
        }
        char ch = content.charAt(bufferPosition++);
        if (Character.isHighSurrogate(ch) && bufferPosition < content.length()) {
            char nextChar = content.charAt(bufferPosition);
            if (Character.isLowSurrogate(nextChar)) {
                ++bufferPosition;
                return Character.toCodePoint(ch, nextChar);
            }
        }
        return ch;
    }

    /**
     * This is used in conjunction with having a preprocessor.
     * We set which lines are actually parsed lines and the
     * unset ones are ignored.
     *
     * @param parsedLines a #java.util.BitSet that holds which lines
     *                    are parsed (i.e. not ignored)
     */
    private void setParsedLines(BitSet parsedLines, boolean reversed) {
        for (int i = 0; i < lineOffsets.length; i++) {
            boolean turnOffLine = !parsedLines.get(i + 1);
            if (reversed)
                turnOffLine = !turnOffLine;
            if (turnOffLine) {
                int lineOffset = lineOffsets[i];
                int nextLineOffset = i < lineOffsets.length - 1 ? lineOffsets[i + 1] : content.length();
                for (int offset = lineOffset; offset < nextLineOffset; offset++) {
                    tokenLocationTable[offset] = IGNORED;
                }
            }
        }
    }

    /**
     * This is used in conjunction with having a preprocessor.
     * We set which lines are actually parsed lines and the
     * unset ones are ignored.
     *
     * @param parsedLines a #java.util.BitSet that holds which lines
     *                    are parsed (i.e. not ignored)
     */
    public void setParsedLines(BitSet parsedLines) {
        setParsedLines(parsedLines, false);
    }

    public void setUnparsedLines(BitSet unparsedLines) {
        setParsedLines(unparsedLines, true);
    }

    /**
     * @return the line number from the absolute offset passed in as a parameter
     */
    public int getLineFromOffset(int pos) {
        if (pos >= content.length()) {
            if (content.charAt(content.length() - 1) == '\n') {
                return startingLine + lineOffsets.length;
            }
            return startingLine + lineOffsets.length - 1;
        }
        int bsearchResult = Arrays.binarySearch(lineOffsets, pos);
        if (bsearchResult >= 0) {
            return Math.max(1, startingLine + bsearchResult);
        }
        return Math.max(1, startingLine - (bsearchResult + 2));
    }

    /**
     * @return the column (1-based and in code points)
     * from the absolute offset passed in as a parameter
     */
    public int getCodePointColumnFromOffset(int pos) {
        if (pos >= content.length()) return 1;
        if (pos == 0) return startingColumn;
        final int line = getLineFromOffset(pos) - startingLine;
        final int lineStart = lineOffsets[line];
        int startColumnAdjustment = line > 0 ? 1 : startingColumn;
        int unadjustedColumn = pos - lineStart + startColumnAdjustment;
        if (!needToCalculateColumns.get(line)) {
            return unadjustedColumn;
        }
        if (Character.isLowSurrogate(content.charAt(pos)))
            --pos;
        int result = startColumnAdjustment;
        for (int i = lineStart; i < pos; i++) {
            char ch = content.charAt(i);
            if (ch == '\t') {
                result += tabSize - (result - 1) % tabSize;
            } else if (Character.isHighSurrogate(ch)) {
                ++result;
                ++i;
            } else {
                ++result;
            }
        }
        return result;
    }

    /**
     * @return the text between startOffset (inclusive)
     * and endOffset(exclusive)
     */
    public String getText(int startOffset, int endOffset) {
        StringBuilder buf = new StringBuilder();
        for (int offset = startOffset; offset < endOffset; offset++) {
            if (tokenLocationTable[offset] != IGNORED) {
                buf.append(content.charAt(offset));
            }
        }
        return buf.toString();
    }

    void cacheToken(Token tok) {
        if (tok.isInserted()) {
            Token next = tok.nextCachedToken();
            if (next != null)
                cacheToken(next);
            return;
        }
        int offset = tok.getBeginOffset();
        if (tokenLocationTable[offset] != IGNORED) {
            tokenOffsets.set(offset);
            tokenLocationTable[offset] = tok;
        }
    }

    void uncacheTokens(Token lastToken) {
        int endOffset = lastToken.getEndOffset();
        if (endOffset < tokenOffsets.length()) {
            tokenOffsets.clear(lastToken.getEndOffset(), tokenOffsets.length());
        }
        lastToken.unsetAppendedToken();
    }

    Token nextCachedToken(int offset) {
        int nextOffset = tokenOffsets.nextSetBit(offset);
        return nextOffset != -1 ? tokenLocationTable[nextOffset] : null;
    }

    Token previousCachedToken(int offset) {
        int prevOffset = tokenOffsets.previousSetBit(offset - 1);
        return prevOffset == -1 ? null : tokenLocationTable[prevOffset];
    }

    private void createLineOffsetsTable() {
        if (content.length() == 0) {
            this.lineOffsets = new int[0];
            return;
        }
        int lineCount = 0;
        int length = content.length();
        for (int i = 0; i < length; i++) {
            char ch = content.charAt(i);
            if (ch == '\t' || Character.isHighSurrogate(ch)) {
                needToCalculateColumns.set(lineCount);
            }
            if (ch == '\n') {
                lineCount++;
            }
        }
        if (content.charAt(length - 1) != '\n') {
            lineCount++;
        }
        int[] lineOffsets = new int[lineCount];
        lineOffsets[0] = 0;
        int index = 1;
        for (int i = 0; i < length; i++) {
            char ch = content.charAt(i);
            if (ch == '\n') {
                if (i + 1 == length) break;
                lineOffsets[index++] = i + 1;
            }
        }
        this.lineOffsets = lineOffsets;
    }

    // Icky method to handle annoying stuff. Might make this public later if it is
    // needed elsewhere
    private static String mungeContent(CharSequence content, boolean preserveTabs, boolean preserveLines, boolean javaUnicodeEscape, boolean ensureFinalEndline) {
        if (preserveTabs && preserveLines && !javaUnicodeEscape) {
            if (ensureFinalEndline) {
                if (content.length() == 0) {
                    content = "\n";
                } else {
                    int lastChar = content.charAt(content.length() - 1);
                    if (lastChar != '\n' && lastChar != '\r') {
                        if (content instanceof StringBuilder) {
                            ((StringBuilder) content).append((char) '\n');
                        } else {
                            content = String.valueOf(content) + '\n';
                        }
                    }
                }
            }
            return content.toString();
        }
        StringBuilder buf = new StringBuilder();
        // This is just to handle tabs to spaces. If you don't have that setting set, it
        // is really unused.
        int col = 0;
        int index = 0, contentLength = content.length();
        while (index < contentLength) {
            char ch = content.charAt(index++);
            if (ch == '\n') {
                buf.append(ch);
                col = 0;
            } else if (javaUnicodeEscape && ch == '\\' && index < contentLength && content.charAt(index) == 'u') {
                int numPrecedingSlashes = 0;
                for (int i = index - 1; i >= 0; i--) {
                    if (content.charAt(i) == '\\')
                        numPrecedingSlashes++;
                    else break;
                }
                if (numPrecedingSlashes % 2 == 0) {
                    buf.append('\\');
                    ++col;
                    continue;
                }
                int numConsecutiveUs = 0;
                for (int i = index; i < contentLength; i++) {
                    if (content.charAt(i) == 'u')
                        numConsecutiveUs++;
                    else break;
                }
                String fourHexDigits = content.subSequence(index + numConsecutiveUs, index + numConsecutiveUs + 4).toString();
                buf.append((char) Integer.parseInt(fourHexDigits, 16));
                index += (numConsecutiveUs + 4);
                ++col;
            } else if (!preserveLines && ch == '\r') {
                buf.append('\n');
                col = 0;
                if (index < contentLength && content.charAt(index) == '\n') {
                    ++index;
                }
            } else if (ch == '\t' && !preserveTabs) {
                int spacesToAdd = DEFAULT_TAB_SIZE - col % DEFAULT_TAB_SIZE;
                for (int i = 0; i < spacesToAdd; i++) {
                    buf.append(' ');
                    col++;
                }
            } else {
                buf.append(ch);
                if (!Character.isLowSurrogate(ch))
                    col++;
            }
        }
        if (ensureFinalEndline) {
            if (buf.length() == 0) {
                return "\n";
            }
            char lastChar = buf.charAt(buf.length() - 1);
            if (lastChar != '\n' && lastChar != '\r')
                buf.append('\n');
        }
        return buf.toString();
    }

    static String displayChar(int ch) {
        if (ch == '\'') return "\'\\'\'";
        if (ch == '\\') return "\'\\\\\'";
        if (ch == '\t') return "\'\\t\'";
        if (ch == '\r') return "\'\\r\'";
        if (ch == '\n') return "\'\\n\'";
        if (ch == '\f') return "\'\\f\'";
        if (ch == ' ') return "\' \'";
        if (ch < 128 && !Character.isWhitespace(ch) && !Character.isISOControl(ch)) return "\'" + (char) ch + "\'";
        if (ch < 10) return "" + ch;
        return "0x" + Integer.toHexString(ch);
    }

    static String addEscapes(String str) {
        StringBuilder retval = new StringBuilder();
        for (int ch : str.codePoints().toArray()) {
            switch (ch) {
                case '\b':
                    retval.append("\\b");
                    continue;
                case '\t':
                    retval.append("\\t");
                    continue;
                case '\n':
                    retval.append("\\n");
                    continue;
                case '\f':
                    retval.append("\\f");
                    continue;
                case '\r':
                    retval.append("\\r");
                    continue;
                case '\"':
                    retval.append("\\\"");
                    continue;
                case '\'':
                    retval.append("\\\'");
                    continue;
                case '\\':
                    retval.append("\\\\");
                    continue;
                default:
                    if (Character.isISOControl(ch)) {
                        String s = "0000" + Integer.toString(ch, 16);
                        retval.append("\\u" + s.substring(s.length() - 4, s.length()));
                    } else {
                        retval.appendCodePoint(ch);
                    }
                    continue;
            }
        }
        return retval.toString();
    }

    // Annoying kludge really...
    static String readToEnd(Reader reader) {
        try {
            return readFully(reader);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    static final int BUF_SIZE = 0x10000;

    static String readFully(Reader reader) throws IOException {
        char[] block = new char[BUF_SIZE];
        int charsRead = reader.read(block);
        if (charsRead < 0) {
            throw new IOException("No input");
        } else if (charsRead < BUF_SIZE) {
            char[] result = new char[charsRead];
            System.arraycopy(block, 0, result, 0, charsRead);
            reader.close();
            return new String(block, 0, charsRead);
        }
        StringBuilder buf = new StringBuilder();
        buf.append(block);
        do {
            charsRead = reader.read(block);
            if (charsRead > 0) {
                buf.append(block, 0, charsRead);
            }
        }
        while (charsRead == BUF_SIZE);
        reader.close();
        return buf.toString();
    }

    /**
     * @param bytes   the raw byte array
     * @param charset The encoding to use to decode the bytes. If this is null, we check for the
     *                initial byte order mark (used by Microsoft a lot seemingly)
     *        See: <a href="https://docs.microsoft.com/es-es/globalization/encoding/byte-order-markc">Microsoft docs</a>
     * @return A String taking into account the encoding passed in or in the byte order mark (if it was present).
     * And if no encoding was passed in and no byte-order mark was present, we assume the raw input
     * is in UTF-8.
     */
    static public String stringFromBytes(byte[] bytes, Charset charset) throws CharacterCodingException {
        int arrayLength = bytes.length;
        if (charset == null) {
            int firstByte = arrayLength > 0 ? Byte.toUnsignedInt(bytes[0]) : 1;
            int secondByte = arrayLength > 1 ? Byte.toUnsignedInt(bytes[1]) : 1;
            int thirdByte = arrayLength > 2 ? Byte.toUnsignedInt(bytes[2]) : 1;
            int fourthByte = arrayLength > 3 ? Byte.toUnsignedInt(bytes[3]) : 1;
            if (firstByte == 0xEF && secondByte == 0xBB && thirdByte == 0xBF) {
                return new String(bytes, 3, bytes.length - 3, Charset.forName("UTF-8"));
            }
            if (firstByte == 0 && secondByte == 0 && thirdByte == 0xFE && fourthByte == 0xFF) {
                return new String(bytes, 4, bytes.length - 4, Charset.forName("UTF-32BE"));
            }
            if (firstByte == 0xFF && secondByte == 0xFE && thirdByte == 0 && fourthByte == 0) {
                return new String(bytes, 4, bytes.length - 4, Charset.forName("UTF-32LE"));
            }
            if (firstByte == 0xFE && secondByte == 0xFF) {
                return new String(bytes, 2, bytes.length - 2, Charset.forName("UTF-16BE"));
            }
            if (firstByte == 0xFF && secondByte == 0xFE) {
                return new String(bytes, 2, bytes.length - 2, Charset.forName("UTF-16LE"));
            }
            charset = UTF_8;
        }
        CharsetDecoder decoder = charset.newDecoder();
        ByteBuffer b = ByteBuffer.wrap(bytes);
        CharBuffer c = CharBuffer.allocate(bytes.length);
        while (true) {
            CoderResult r = decoder.decode(b, c, false);
            if (!r.isError()) {
                break;
            }
            if (!r.isMalformed()) {
                r.throwException();
            }
            int n = r.length();
            b.position(b.position() + n);
            for (int i = 0; i < n; i++) {
                c.put((char) 0xFFFD);
            }
        }
        ((Buffer) c).limit(c.position());
        ((Buffer) c).rewind();
        return c.toString();
        // return new String(bytes, charset);
    }

    static public String stringFromBytes(byte[] bytes) throws CharacterCodingException {
        return stringFromBytes(bytes, null);
    }

}


