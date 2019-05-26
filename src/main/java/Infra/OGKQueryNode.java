package Infra;

public class OGKQueryNode {

    private NodeType nodeType;
    private String typeString;

    public OGKQueryNode(NodeType nodeType, String typeString) {

        this.nodeType = nodeType;
        this.typeString = typeString;

    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getTypeString() {
        return typeString;
    }

}
