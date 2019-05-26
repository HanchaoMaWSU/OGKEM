package Infra;

import java.util.*;


public class QueriesDependencyGraph {


    private HashMap<String, OGKQuery> queries;
    private OGKQuery root;
    public  HashMap<String,HashMap<String,String>> childrenMap;
    public  HashMap<String,HashMap<String,String>> parentMap;

    public QueriesDependencyGraph(HashMap<String, OGKQuery> queries,String keyRoot) {

        this.queries = queries;
        this.root = queries.get(keyRoot);
        childrenMap  = new HashMap<>();
        parentMap = new HashMap<>();
        buildDependency();

    }

    private void buildDependency() {

        for (String type : queries.keySet()) {

            for (String pairwiseType : queries.keySet()) {

                if (type.equals(pairwiseType)) {

                    continue;

                } else {

                    if (queries.get(type).getQueryNodeMap().containsKey(pairwiseType)) {

                        if (!childrenMap.containsKey(type)){
                            childrenMap.put(type,new HashMap<String,String>());
                        }
                        if (!parentMap.containsKey(pairwiseType)){
                            parentMap.put(pairwiseType,new HashMap<String,String>());
                        }

                        OGKQuery parent = queries.get(type);
                        OGKQuery child = queries.get(pairwiseType);

                        if (! parent.getRoot().equals(child.getRoot())) {

                            parent.getChildren().add(child);
                            child.getParents().add(parent);

                            for (OGKQueryEdge queryEdge : parent.getQueryEdgeList()) {
                                if (type.equals(queryEdge.getSrc()) && pairwiseType.equals(queryEdge.getDst())) {
                                    childrenMap.get(type).put(pairwiseType,queryEdge.getEdgePredicate());
                                    parentMap.get(pairwiseType).put(type,queryEdge.getEdgePredicate());
                                }
                            }

                        }

                    }

                }

            }

        }

    }

    public HashMap<String, OGKQuery> getQueries() {
        return queries;
    }
    public OGKQuery getRoot() { return root; }

}