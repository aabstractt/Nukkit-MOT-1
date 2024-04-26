package cn.nukkit.nbt.snbt;

import cn.nukkit.nbt.snbt.ast.Delimiter;
import cn.nukkit.nbt.snbt.ast.Literal;
import cn.nukkit.nbt.snbt.ast.WHITESPACE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class Token implements SNBTConstants, Node {
    private TokenType type;
    private SNBTLexer tokenSource;
    private int beginOffset, endOffset;
    private boolean unparsed;
    private Node parent;
    private String image;

    public void setImage(String image) {
        this.image = image;
    }

    private Token prependedToken, appendedToken;
    private boolean inserted;

    public boolean isInserted() {
        return inserted;
    }

    public void preInsert(Token prependedToken) {
        if (prependedToken == this.prependedToken) return;
        prependedToken.appendedToken = this;
        Token existingPreviousToken = this.previousCachedToken();
        if (existingPreviousToken != null) {
            existingPreviousToken.appendedToken = prependedToken;
            prependedToken.prependedToken = existingPreviousToken;
        }
        prependedToken.inserted = true;
        prependedToken.beginOffset = prependedToken.endOffset = this.beginOffset;
        this.prependedToken = prependedToken;
    }

    void unsetAppendedToken() {
        this.appendedToken = null;
    }

    /**
     * @param type        the #TokenType of the token being constructed
     * @param image       the String content of the token
     * @param tokenSource the object that vended this token.
     */
    public Token(TokenType type, String image, SNBTLexer tokenSource) {
        this.type = type;
        this.image = image;
        this.tokenSource = tokenSource;
    }

    public static Token newToken(TokenType type, String image, SNBTLexer tokenSource) {
        Token result = newToken(type, tokenSource, 0, 0);
        result.setImage(image);
        return result;
    }

    /**
     * It would be extremely rare that an application
     * programmer would use this method. It needs to
     * be public because it is part of the cn.nukkit.nbt.snbt.Node interface.
     */
    @Override
    public void setBeginOffset(int beginOffset) {
        this.beginOffset = beginOffset;
    }

    /**
     * It would be extremely rare that an application
     * programmer would use this method. It needs to
     * be public because it is part of the cn.nukkit.nbt.snbt.Node interface.
     */
    @Override
    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    /**
     * @return the SNBTLexer object that handles
     * location info for the tokens.
     */
    @Override
    public SNBTLexer getTokenSource() {
        SNBTLexer flm = this.tokenSource;
        // If this is null and we have chained tokens,
        // we try to get it from there! (Why not?)
        if (flm == null) {
            if (prependedToken != null) {
                flm = prependedToken.getTokenSource();
            }
            if (flm == null && appendedToken != null) {
                flm = appendedToken.getTokenSource();
            }
        }
        return flm;
    }

    /**
     * It should be exceedingly rare that an application
     * programmer needs to use this method.
     */
    @Override
    public void setTokenSource(SNBTLexer tokenSource) {
        this.tokenSource = tokenSource;
    }

    /**
     * Return the TokenType of this Token object
     */
    public TokenType getType() {
        return type;
    }

    protected void setType(TokenType type) {
        this.type = type;
    }

    /**
     * @return whether this Token represent actual input or was it inserted somehow?
     */
    public boolean isVirtual() {
        return type == TokenType.EOF;
    }

    /**
     * @return Did we skip this token in parsing?
     */
    public boolean isSkipped() {
        return false;
    }

    @Override
    public int getBeginOffset() {
        return beginOffset;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }

    /**
     * @return the string image of the token.
     */
    public String getImage() {
        return image != null ? image : getSource();
    }

    /**
     * @return the next _cached_ regular (i.e. parsed) token
     * or null
     */
    public final Token getNext() {
        return getNextParsedToken();
    }

    /**
     * @return the previous regular (i.e. parsed) token
     * or null
     */
    public final Token getPrevious() {
        Token result = previousCachedToken();
        while (result != null && result.isUnparsed()) {
            result = result.previousCachedToken();
        }
        return result;
    }

    /**
     * @return the next regular (i.e. parsed) token
     */
    private Token getNextParsedToken() {
        Token result = nextCachedToken();
        while (result != null && result.isUnparsed()) {
            result = result.nextCachedToken();
        }
        return result;
    }

    /**
     * @return the next token of any sort (parsed or unparsed or invalid)
     */
    public Token nextCachedToken() {
        if (getType() == TokenType.EOF) return null;
        if (appendedToken != null) return appendedToken;
        SNBTLexer tokenSource = getTokenSource();
        return tokenSource != null ? tokenSource.nextCachedToken(getEndOffset()) : null;
    }

    public Token previousCachedToken() {
        if (prependedToken != null) return prependedToken;
        if (getTokenSource() == null) return null;
        return getTokenSource().previousCachedToken(getBeginOffset());
    }

    Token getPreviousToken() {
        return previousCachedToken();
    }

    public Token replaceType(TokenType type) {
        Token result = newToken(type, getTokenSource(), getBeginOffset(), getEndOffset());
        result.prependedToken = this.prependedToken;
        result.appendedToken = this.appendedToken;
        result.inserted = this.inserted;
        if (result.appendedToken != null) {
            result.appendedToken.prependedToken = result;
        }
        if (result.prependedToken != null) {
            result.prependedToken.appendedToken = result;
        }
        if (!result.inserted) {
            getTokenSource().cacheToken(result);
        }
        return result;
    }

    @Override
    public String getSource() {
        if (type == TokenType.EOF) return "";
        SNBTLexer flm = getTokenSource();
        return flm == null ? null : flm.getText(getBeginOffset(), getEndOffset());
    }

    protected Token() {
    }

    public Token(TokenType type, SNBTLexer tokenSource, int beginOffset, int endOffset) {
        this.type = type;
        this.tokenSource = tokenSource;
        this.beginOffset = beginOffset;
        this.endOffset = endOffset;
    }

    @Override
    public boolean isUnparsed() {
        return unparsed;
    }

    @Override
    public void setUnparsed(boolean unparsed) {
        this.unparsed = unparsed;
    }

    @Override
    public void clearChildren() {
    }

    public String getNormalizedText() {
        if (getType() == TokenType.EOF) {
            return "EOF";
        }
        return getImage();
    }

    public String toString() {
        return getNormalizedText();
    }

    /**
     * @return An iterator of the tokens preceding this one.
     */
    public Iterator<Token> precedingTokens() {
        return new Iterator<Token>() {
            Token currentPoint = Token.this;

            @Override
            public boolean hasNext() {
                return currentPoint.previousCachedToken() != null;
            }

            @Override
            public Token next() {
                Token previous = currentPoint.previousCachedToken();
                if (previous == null) throw new java.util.NoSuchElementException("No previous token!");
                return currentPoint = previous;
            }

        }
                ;
    }

    /**
     * @return a list of the unparsed tokens preceding this one in the order they appear in the input
     */
    public List<Token> precedingUnparsedTokens() {
        List<Token> result = new ArrayList<>();
        Token t = this.previousCachedToken();
        while (t != null && t.isUnparsed()) {
            result.add(t);
            t = t.previousCachedToken();
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * @return An iterator of the (cached) tokens that follow this one.
     */
    public Iterator<Token> followingTokens() {
        return new Iterator<Token>() {
            Token currentPoint = Token.this;

            @Override
            public boolean hasNext() {
                return currentPoint.nextCachedToken() != null;
            }

            @Override
            public Token next() {
                Token next = currentPoint.nextCachedToken();
                if (next == null) throw new java.util.NoSuchElementException("No next token!");
                return currentPoint = next;
            }

        }
                ;
    }

    /**
     * Copy the location info from a Node
     */
    @Override
    public void copyLocationInfo(Node from) {
        Node.super.copyLocationInfo(from);
        if (from instanceof Token) {
            Token otherTok = (Token) from;
            appendedToken = otherTok.appendedToken;
            prependedToken = otherTok.prependedToken;
        }
        setTokenSource(from.getTokenSource());
    }

    @Override
    public void copyLocationInfo(Node start, Node end) {
        Node.super.copyLocationInfo(start, end);
        if (start instanceof Token) {
            prependedToken = ((Token) start).prependedToken;
        }
        if (end instanceof Token) {
            Token endToken = (Token) end;
            appendedToken = endToken.appendedToken;
        }
    }

    public static Token newToken(TokenType type, SNBTLexer tokenSource, int beginOffset, int endOffset) {
        switch (type) {
            case WHITESPACE:
                return new WHITESPACE(TokenType.WHITESPACE, tokenSource, beginOffset, endOffset);
            case COLON:
                return new Delimiter(TokenType.COLON, tokenSource, beginOffset, endOffset);
            case COMMA:
                return new Delimiter(TokenType.COMMA, tokenSource, beginOffset, endOffset);
            case OPEN_BRACKET:
                return new Delimiter(TokenType.OPEN_BRACKET, tokenSource, beginOffset, endOffset);
            case CLOSE_BRACKET:
                return new Delimiter(TokenType.CLOSE_BRACKET, tokenSource, beginOffset, endOffset);
            case OPEN_BRACE:
                return new Delimiter(TokenType.OPEN_BRACE, tokenSource, beginOffset, endOffset);
            case CLOSE_BRACE:
                return new Delimiter(TokenType.CLOSE_BRACE, tokenSource, beginOffset, endOffset);
            case BOOLEAN:
                return new Literal(TokenType.BOOLEAN, tokenSource, beginOffset, endOffset);
            case FLOAT:
                return new Literal(TokenType.FLOAT, tokenSource, beginOffset, endOffset);
            case DOUBLE:
                return new Literal(TokenType.DOUBLE, tokenSource, beginOffset, endOffset);
            case INTEGER:
                return new Literal(TokenType.INTEGER, tokenSource, beginOffset, endOffset);
            case LONG:
                return new Literal(TokenType.LONG, tokenSource, beginOffset, endOffset);
            case BYTE:
                return new Literal(TokenType.BYTE, tokenSource, beginOffset, endOffset);
            case SHORT:
                return new Literal(TokenType.SHORT, tokenSource, beginOffset, endOffset);
            case STRING:
                return new Literal(TokenType.STRING, tokenSource, beginOffset, endOffset);
            case INVALID:
                return new InvalidToken(tokenSource, beginOffset, endOffset);
            default:
                return new Token(type, tokenSource, beginOffset, endOffset);
        }
    }

    @Override
    public String getLocation() {
        return getInputSource() + ":" + getBeginLine() + ":" + getBeginColumn();
    }

    @Override
    public void setChild(int i, Node n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChild(Node n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChild(int i, Node n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node removeChild(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int indexOf(Node n) {
        return -1;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public void setParent(Node parent) {
        this.parent = parent;
    }

    @Override
    public final int getChildCount() {
        return 0;
    }

    @Override
    public final Node getChild(int i) {
        return null;
    }

    @Override
    public final List<Node> children() {
        return Collections.emptyList();
    }

}


