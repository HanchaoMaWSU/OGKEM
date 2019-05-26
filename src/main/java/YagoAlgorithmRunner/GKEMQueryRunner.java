package YagoAlgorithmRunner;

import YagoOntology.YagoOntologyGraph;
import Infra.*;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.*;


public class GKEMQueryRunner {

    private QueriesDependencyGraph queryGraph;
    private YagoOntologyGraph oGraph;
    private double theta;
    private YagoInduceGraphGenerator induceGraphs;

    public GKEMQueryRunner(QueriesDependencyGraph queryGraph,
                           YagoOntologyGraph oGraph, double theta,
                           YagoInduceGraphGenerator induceGraphs) {

        this.oGraph = oGraph;
        this.theta = theta;
        this.queryGraph = queryGraph;
        this.induceGraphs = induceGraphs;

    }

    public void execute() {

        if (queryGraph.getQueries() == null || queryGraph.getQueries().size() == 0) {
            System.out.println("No Input Queries!!!");
            return;
        }

        long startTime = System.nanoTime();

        Queue<OGKQuery> queue = new LinkedList<>();
        queue.add(queryGraph.getRoot());

        while (queue.size() != 0) {

            OGKQuery currentQuery = queue.remove();
            getInitialMatches(currentQuery);

            if (currentQuery.getChildren().size() != 0) {
                for (OGKQuery child : currentQuery.getChildren()) {
                    queue.add(child);
                }
            }

        }


        refineChase();

        long endTime = System.nanoTime();

        System.out.println("OGKEM Time: " + (endTime - startTime));
        ArrayList<HashSet<DataNode>> finalPartition = generateFinalPartition(queryGraph.getRoot().getResultMatches().
                classSet);

        int total;
        ArrayList<HashSet<DataNode>> finalClasses = getFinalClasses(finalPartition,
                induceGraphs.graphMaps.get(queryGraph.getRoot().getRoot()), queryGraph.getRoot().edgePairs);

        System.out.println("Final classes:");
        total = 0;
        for (int i = 0; i < finalClasses.size(); i++) {
            total += finalClasses.get(i).size();
            System.out.println("------------");
            for (DataNode node :  finalClasses.get(i)) {
                System.out.println(node.getNodeName());
                System.out.println("Theta is : " + queryGraph.getRoot().getResultMatches().costMap.get(node));
            }
            System.out.println("------------");

        }
        System.out.println(total + "Total classes");

    }

    private ArrayList<HashSet<DataNode>> getFinalClasses(ArrayList<HashSet<DataNode>> finalPartition,
                                                         DefaultDirectedGraph<DataNode, RelationshipEdge> induceGraph,
                                                         HashMap<String, String> edgePairs) {

        ArrayList<HashSet<DataNode>> finalClasses = new ArrayList<>();

        for (int i = 0; i <  finalPartition.size(); i++) {

            HashSet<DataNode> currentPartition = finalPartition.get(i);
            ArrayList<HashSet<DataNode>> currentClasses = new ArrayList<>();
            HashSet<DataNode> visited = new HashSet<>();

            for (DataNode nodeA : currentPartition) {

                if (visited.contains(nodeA)) {
                    continue;
                }
                visited.add(nodeA);
                HashSet<DataNode> currentClass = new HashSet<>();
                currentClass.add(nodeA);

                for (DataNode nodeB :  currentPartition) {


                    if (nodeA.equals(nodeB)|| visited.contains(nodeB)) {
                        continue;
                    }

                    if (isBisimilar(nodeA, nodeB,induceGraph,edgePairs)) {
                        currentClass.add(nodeB);
                        visited.add(nodeB);
                    }


                }
                currentClasses.add(new HashSet<>(currentClass));

            }
            finalClasses.addAll(new ArrayList<>(currentClasses));
        }



        return finalClasses;
    }

    private boolean isBisimilar(DataNode nodeA, DataNode nodeB, DefaultDirectedGraph<DataNode,
            RelationshipEdge> induceGraph, HashMap<String, String> edgePairs) {

        for (String key :edgePairs.keySet()) {
            if (key.equals(edgePairs.get(key))) {
                DataNode dstA = null;
                DataNode dstB = null;
                for (RelationshipEdge edge : induceGraph.outgoingEdgesOf(nodeA)) {
                    if (edge.getLabel().equals(key)) {
                        dstA = induceGraph.getEdgeTarget(edge);
                    }
                }

                for (RelationshipEdge edge : induceGraph.outgoingEdgesOf(nodeB)) {
                    if (edge.getLabel().equals(key)) {
                        dstB = induceGraph.getEdgeTarget(edge);
                    }
                }
                if (!isSameValue(dstA,dstB)) {
                    return false;
                }
            }

        }

        return true;

    }

    private boolean isSameValue(DataNode dstA, DataNode dstB) {

        if (dstA == null || dstB == null) {
            return false;
        }
        return dstA.getNodeName().equals(dstB.getNodeName());
    }

    private ArrayList<HashSet<DataNode>> generateFinalPartition(HashMap<DataNode,HashMap<Integer,Integer>> classSet) {

        ArrayList<HashSet<DataNode>> classes = new ArrayList<>();

        HashSet<DataNode> visited = new HashSet<>();
        for (DataNode nodeA :  classSet.keySet()) {

            if (visited.contains(nodeA)) {
                continue;
            }
            visited.add(nodeA);
            HashSet<DataNode> currentClass = new HashSet<>();
            currentClass.add(nodeA);

            for (DataNode nodeB :  classSet.keySet()) {


                if (nodeA.equals(nodeB)|| visited.contains(nodeB)) {
                    continue;
                }

                if (hasSameEids(classSet.get(nodeA), classSet.get(nodeB))) {
                    currentClass.add(nodeB);
                    visited.add(nodeB);
                }


            }
            classes.add(new HashSet<>(currentClass));
        }
        return classes;

    }

    private boolean hasSameEids(HashMap<Integer, Integer> map1, HashMap<Integer, Integer> map2) {

        if (map1.size() == 0 || map2.size() == 0) {
            return false;
        }

        if (map1.size() != map2.size()) {
            return false;
        }

        boolean result = true;
        for (int key : map1.keySet()) {
            if (!map2.containsKey(key)) {
                return false;
            }
            result = result && map1.get(key) == map2.get(key);
        }
        return result;

    }
    private void getInitialMatches(OGKQuery currentQuery) {

        if (!currentQuery.isC) {

            currentQuery.initialMatchSet(currentQuery.eid);
            HashMap<String, String> edgePairs = currentQuery.edgePairs;




            DefaultDirectedGraph<DataNode,RelationshipEdge> induceGraph =
                    induceGraphs.graphMaps.get(currentQuery.getRoot());


            for (DataNode dataNode : induceGraph.vertexSet()) {

                if (dataNode.getType().equals(dataNode.getType()) ) {

                    boolean isValidCandidate = true;
                    HashMap<String,Boolean> factors = new HashMap();
                    for(String key : edgePairs.keySet()) {
                        factors.put(key,false);
                    }

                    for (RelationshipEdge edge : induceGraph.outgoingEdgesOf(dataNode)) {
                        if (edgePairs.containsKey(edge.getLabel()) ) {
                            if (edgePairs.get(edge.getLabel()).equals(edge.getLabel())) {
                                if (induceGraph.getEdgeTarget(edge).getNodeType() == NodeType.Literal) {
                                    factors.put(edge.getLabel(), true);
                                }
                            } else {
                                if (edgePairs.get(edge.getLabel()).equals(induceGraph.getEdgeTarget(edge).getType()) ) {
                                    factors.put(edge.getLabel(), true);
                                }

                            }



                        }

                    }
                    for (String key : factors.keySet()) {
                        isValidCandidate = isValidCandidate && factors.get(key);
                    }
                    if (isValidCandidate) {
                        currentQuery.getResultMatches().classSet.put(dataNode, new HashMap<Integer, Integer>());
                    }

                }

            }
            System.out.println(currentQuery.getRoot());
            System.out.println(currentQuery.getResultMatches().classSet.size() + " " + "initial matches.");

        } else  {

            currentQuery.initialMatchSet(currentQuery.eid);
            DefaultDirectedGraph<DataNode,RelationshipEdge> induceGraph =
                    induceGraphs.graphMaps.get(currentQuery.getRoot());

            HashMap<String, String> edgePairs = currentQuery.edgePairs;

            for (DataNode node : induceGraph.vertexSet()) {
                if (currentQuery.getRoot().equals(node.getType())) {
                    currentQuery.getResultMatches().classSet.put(node, new HashMap<Integer, Integer>());
                }
            }
            HashMap<DataNode, String> matches =  new HashMap<>();
            ArrayList<DataNode> needToRemove = new ArrayList<>();

            for (String edgePredicate : edgePairs.keySet()) {
                for (DataNode candidate : currentQuery.getResultMatches().classSet.keySet()) {
                    boolean canBePruned = true;
                    for (RelationshipEdge edge : induceGraph.outgoingEdgesOf(candidate)) {
                        if (edge.getLabel().equals(edgePredicate) &&
                                induceGraph.getEdgeTarget(edge).getNodeType() == NodeType.Literal) {
                            canBePruned = false;
                            matches.put(candidate,  induceGraph.getEdgeTarget(edge).getNodeName());
                            break;
                        }


                    }
                    if (canBePruned) {
                        needToRemove.add(candidate);
                    }
                }
                for (int i = 0; i < needToRemove.size(); i++) {
                    currentQuery.getResultMatches().classSet.remove(needToRemove.get(i));
                }
                currentQuery.getResultMatches().partitionResultSets(matches);


            }
            System.out.println(currentQuery.getRoot());
            System.out.println(currentQuery.getResultMatches().classSet.size() + " " + "initial matches.");

        }
    }

    private void refineChase() {


        Queue<OGKQuery> queue = new LinkedList<>();

        for (String root : queryGraph.getQueries().keySet()) {
            OGKQuery ogkQuery = queryGraph.getQueries().get(root);
            if (ogkQuery.getChildren().size() == 0 && ogkQuery.isC) {
                if (ogkQuery.getResultMatches() != null) {
                    queue.add(ogkQuery);
                }

            }
        }

        while (queue.size() != 0) {

            int size = queue.size();

            for (int i = 0; i < size; i++) {
                OGKQuery currentQuery = queue.remove();
                if (currentQuery.getParents().size() != 0) {
                    for (OGKQuery parent : currentQuery.getParents()) {
                        if (parent.getResultMatches() != null && currentQuery.getResultMatches() != null) {
                            parent.getResultMatches().chaseByChild(currentQuery.getResultMatches(),
                                    induceGraphs.graphMaps.get(parent.getRoot()));
                        } else {
                            System.out.println("Missing Partial Match!");
                        }
                        if (parent.getParents().size() != 0) {
                            queue.add(parent);
                        }

                    }

                }

            }

        }



        MatchResult result = queryGraph.getRoot().getResultMatches();
        System.out.print(queryGraph.getRoot().getRoot() + " ");
        if (result == null) {
            System.out.println("Empty Classes!");
            return;
        }

        System.out.println(result.classSet.size() + " Classes.");

        for (DataNode dataNode : result.classSet.keySet()) {
            HashMap<Integer, Integer> eids = result.classSet.get(dataNode);
            System.out.println("Name : " + dataNode.getNodeName());
            for (Integer eid : eids.keySet()) {
                System.out.print("Eid " + eid + " : ");
                System.out.println(eids.get(eid));
            }
            System.out.println("--------");
        }

    }



}
