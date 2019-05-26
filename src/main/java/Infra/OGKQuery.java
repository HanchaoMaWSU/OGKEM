package Infra;

import  java.util.*;
public class OGKQuery {

    private String root;
    private HashMap<String, String> literals;
    private HashMap<String, OGKQueryNode> queryNodeMap;
    private ArrayList<OGKQueryEdge> queryEdgeList;
    private MatchResult resultMatches;
    private ArrayList<OGKQuery> children;
    private ArrayList<OGKQuery> parents;
    public boolean isC = true;
    public boolean isAllVariables = true;
    public boolean isSubTree = false;
    public boolean isVal = false;
    public int eid;
    public double ud;
    public HashMap<String, String> edgePairs;


    public OGKQuery(String root, HashMap<String, OGKQueryNode> queryNodeMap,
                    ArrayList<OGKQueryEdge> queryEdgeList,HashMap<String, String> literals,
                    boolean isSubTree, boolean isVal) {

        this.root = root;
        this.literals = literals;
        this.queryNodeMap = queryNodeMap;
        this.queryEdgeList = queryEdgeList;
        this.isVal = isVal;
        this.isSubTree = isSubTree;this.eid = root.hashCode();
        children = new ArrayList<>();
        parents = new ArrayList<>();
        edgePairs = new HashMap<>();
        this.eid = root.hashCode();
        this.ud = 0.0;

        for (OGKQueryEdge queryEdge : queryEdgeList) {
            if (!queryEdge.getDst().equals(queryEdge.getEdgePredicate())) {
                isC = false;
                break;
            }

        }

        for (OGKQueryEdge queryEdge : queryEdgeList) {
            if (queryEdge.getDst().equals(queryEdge.getEdgePredicate())) {
                isAllVariables = false;
                break;
            }
        }


        for (OGKQueryEdge edge : queryEdgeList) {
            edgePairs.put(edge.getEdgePredicate(),edge.getDst());
        }

    }

    public HashMap<String, String> getLiterals() { return literals; }

    public String getRoot() {
        return root;
    }

    public ArrayList<OGKQuery> getChildren() {
        return children;
    }

    public ArrayList<OGKQuery> getParents() {
        return parents;
    }

    public HashMap<String, OGKQueryNode> getQueryNodeMap() { return queryNodeMap; }

    public ArrayList<OGKQueryEdge> getQueryEdgeList() { return queryEdgeList; }

    public MatchResult getResultMatches() {
        return resultMatches;
    }

    public void initialMatchSet(int eid) {
        resultMatches = new MatchResult(eid,root);
    }

    public boolean isSubTree() {
        return isSubTree;
    }

    public void setIsSubTree(boolean subTree) {
        isSubTree = subTree;
    }

    public boolean isC() {
        return isC;
    }

    public void setC(boolean c) {
        isC = c;
    }

    public boolean isAllVariables() {
        return isAllVariables;
    }

    public void setAllVariables(boolean allVariables) {
        isAllVariables = allVariables;
    }


}
