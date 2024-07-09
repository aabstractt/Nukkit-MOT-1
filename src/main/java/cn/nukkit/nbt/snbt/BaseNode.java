package cn.nukkit.nbt.snbt;

import java.util.*;


/**
 * The base concrete class for non-terminal Nodes
 */
public class BaseNode implements Node {
    private SNBTLexer tokenSource;

    @Override
    public SNBTLexer getTokenSource() {
        if (tokenSource == null) {
            for (Node child : children()) {
                tokenSource = child.getTokenSource();
                if (tokenSource != null) break;
            }
        }
        return tokenSource;
    }

    @Override
    public void setTokenSource(SNBTLexer tokenSource) {
        this.tokenSource = tokenSource;
    }

    static private Class<? extends List> listClass;

    /**
     * Sets the List class that is used to store child nodes. By default,
     * this is java.util.ArrayList. There is probably very little reason
     * to ever use anything else, though you could use this method
     * to replace this with LinkedList or your own java.util.List implementation even.
     *
     * @param listClass the #java.util.List implementation to use internally
     *                  for the child nodes. By default #java.util.ArrayList is used.
     */
    static public void setListClass(Class<? extends List> listClass) {
        BaseNode.listClass = listClass;
    }

    @SuppressWarnings("unchecked")
    private List<Node> newList() {
        if (listClass == null) {
            return new ArrayList<>();
        }
        try {
            return (List<Node>) listClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * the parent node
     */
    private Node parent;
    /**
     * the child nodes
     */
    private List<Node> children = newList();
    private int beginOffset, endOffset;
    private boolean unparsed;

    @Override
    public boolean isUnparsed() {
        return this.unparsed;
    }

    @Override
    public void setUnparsed(boolean unparsed) {
        this.unparsed = unparsed;
    }

    @Override
    public void setParent(Node n) {
        parent = n;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public void addChild(Node n) {
        children.add(n);
        n.setParent(this);
    }

    @Override
    public void addChild(int i, Node n) {
        children.add(i, n);
        n.setParent(this);
    }

    @Override
    public Node getChild(int i) {
        return children.get(i);
    }

    @Override
    public void setChild(int i, Node n) {
        children.set(i, n);
        n.setParent(this);
    }

    @Override
    public Node removeChild(int i) {
        return children.remove(i);
    }

    @Override
    public void clearChildren() {
        children.clear();
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public List<Node> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public int getBeginOffset() {
        return beginOffset;
    }

    @Override
    public void setBeginOffset(int beginOffset) {
        this.beginOffset = beginOffset;
    }

    @Override
    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public void setEndOffset(int endOffset) {
        this.endOffset = endOffset;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Token t : getRealTokens()) {
            buf.append(t);
        }
        return buf.toString();
    }

    private Map<String, Node> namedChildMap;
    private Map<String, List<Node>> namedChildListMap;

    public Node getNamedChild(String name) {
        if (namedChildMap == null) {
            return null;
        }
        return namedChildMap.get(name);
    }

    public void setNamedChild(String name, Node node) {
        if (namedChildMap == null) {
            namedChildMap = new HashMap<>();
        }
        if (namedChildMap.containsKey(name)) {
            // Can't have duplicates
            String msg = String.format("Duplicate named child not allowed: {0}", name);
            throw new RuntimeException(msg);
        }
        namedChildMap.put(name, node);
    }

    public List<Node> getNamedChildList(String name) {
        if (namedChildListMap == null) {
            return null;
        }
        return namedChildListMap.get(name);
    }

    public void addToNamedChildList(String name, Node node) {
        if (namedChildListMap == null) {
            namedChildListMap = new HashMap<>();
        }
        List<Node> nodeList = namedChildListMap.get(name);
        if (nodeList == null) {
            nodeList = new ArrayList<>();
            namedChildListMap.put(name, nodeList);
        }
        nodeList.add(node);
    }

}


