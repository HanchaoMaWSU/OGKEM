package Infra;

import java.util.HashSet;

public class DataNode {

    private String nodeName;
    private NodeType nodeType;
    private String type = "";
    private HashSet<String> similarTypesForPatternNode;
    private HashSet<String> types;

    public DataNode(String nodeName, NodeType nodeType, String type) {

        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.similarTypesForPatternNode = new HashSet<>();
        this.types = new HashSet<>();
        this.type = type;

    }

    public String getNodeName() {
        return nodeName;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public HashSet<String> getSimilarTypesForPatternNode() {
        return similarTypesForPatternNode;
    }

    public HashSet<String> getTypes() { return types; }

    public void addSimilarTypes(String type) {
        this.similarTypesForPatternNode.add(type);
    }

    public void addTypes(String type) { this.types.add(type);}

    public String getType() { return type; }

    public void setType(String type) {
        this.type = type;
    }

}
