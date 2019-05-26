package Infra;

import java.util.*;

public class ONode {

    private String labelString;
    private HashSet<ONode> children;
    private HashSet<ONode> parents;

    public ONode(String labelString) {

        this.labelString = labelString;
        children = new HashSet<>();
        parents = new HashSet<>();

    }

    @Override
    public boolean equals(Object o) {

        if (o == null || !(o instanceof ONode)) {
            return false;
        }

        if (o == this) {
            return true;
        }

        ONode currentOntology = (ONode) o;

        return currentOntology.labelString.equals(this.labelString);

    }

    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + labelString.hashCode();

        return result;

    }

    public void addChild(ONode child) {
        children.add(child);
    }

    public void addParent(ONode parent) {
        parents.add(parent);
    }

    public HashSet<ONode> getChildren() {
        return children;
    }

    public HashSet<ONode> getParents() {
        return parents;
    }

    public String getLabelString() {
        return labelString;
    }

}
