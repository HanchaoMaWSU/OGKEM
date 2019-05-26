package YagoAlgorithmRunner;

import java.util.*;

import Infra.*;
import YagoGraph.*;
import YagoOntology.*;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;;
import java.util.Comparator;

public class OGKEMQueryRunner {

    private QueriesDependencyGraph queryGraph;
    private YagoDataGraph dataGraph;
    private YagoOntologyGraph oGraph;
    private VF2AbstractIsomorphismInspector<DataNode, RelationshipEdge> inspector;
    private Comparator<DataNode> myVertexOntologyComparator;
    private Comparator<RelationshipEdge> myEdgeComparator;
    private double theta;
    private int alpha;
    private YagoInduceGraphGenerator induceGraphs;

    public OGKEMQueryRunner(QueriesDependencyGraph queryGraph, YagoDataGraph dataGraph,
                            YagoOntologyGraph oGraph, double theta, int alpha,
                            YagoInduceGraphGenerator induceGraphs) {

        this.dataGraph = dataGraph;
        this.oGraph = oGraph;
        this.theta = theta;
        this.alpha = alpha;
        this.queryGraph = queryGraph;
        this.induceGraphs = induceGraphs;

        myEdgeComparator = new Comparator<RelationshipEdge>() {
            @Override
            public int compare(RelationshipEdge o1, RelationshipEdge o2) {

                if (o1.getLabel().equals(o2.getLabel())) {
                    return 0;
                } else {
                    return 1;
                }

            }

        };

        myVertexOntologyComparator = new Comparator<DataNode>() {
            @Override
            public int compare(DataNode o1, DataNode o2) {

                if (o2.getNodeType() == o1.getNodeType()) {

                    if (o2.getNodeType() == NodeType.Literal && o1.getNodeType() == NodeType.Literal) {

                        return 0;

                    } else if (hasIntersection(o2.getSimilarTypesForPatternNode(), o1.getTypes())
                            || o1.getTypes().contains(o2.getNodeName())) {

                        return 0;

                    } else {

                        return 1;

                    }

                } else {

                    return 1;

                }

            }

        };

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
        pruneByTheta(queryGraph.getRoot(),theta);
        long endTime = System.nanoTime();

        System.out.println("OGKEM Time: " + (endTime - startTime));
        ArrayList<HashSet<DataNode>> finalPartition = generateFinalPartition(queryGraph.getRoot().getResultMatches().
                classSet);

        System.out.println("Final Partition:");
        int total = 0;
        for (int i = 0; i < finalPartition.size(); i++) {
            total += finalPartition.get(i).size();
//            System.out.println("------------");
            for (DataNode node :  finalPartition.get(i)) {
//                System.out.println(node.getNodeName());
            }
//            System.out.println("------------");

        }
        System.out.println(total + "Total");

        ArrayList<HashSet<DataNode>> finalClasses = getFinalClasses(finalPartition,
                induceGraphs.graphMaps.get(queryGraph.getRoot().getRoot()), queryGraph.getRoot().edgePairs);

        System.out.println("Final classes:");
        total = 0;
        for (int i = 0; i < finalClasses.size(); i++) {
            total += finalClasses.get(i).size();
            System.out.println("------------");
            for (DataNode node :  finalClasses.get(i)) {
//                System.out.println(node.getNodeName());
//                System.out.println("Theta is : " + queryGraph.getRoot().getResultMatches().costMap.get(node));
            }
//            System.out.println("------------");

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
        System.out.println("Names:");
        System.out.println(dstA.getNodeName());
        System.out.println(dstB.getNodeName());
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
        System.out.print(currentQuery.getRoot());
        if (!currentQuery.isC) {

            currentQuery.initialMatchSet(currentQuery.eid);
            HashSet<String> similarLabelSrc = oGraph.getSimilarLabelSet(currentQuery.getRoot(),alpha);
            HashMap<String, String> edgePairs = currentQuery.edgePairs;




            DefaultDirectedGraph<DataNode,RelationshipEdge> induceGraph =
                    induceGraphs.graphMaps.get(currentQuery.getRoot());


            for (DataNode dataNode : induceGraph.vertexSet()) {

                if (similarLabelSrc.contains(dataNode.getType()) ) {
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
                                HashSet<String> similarLabelDst =
                                        oGraph.getSimilarLabelSet(edgePairs.get(edge.getLabel()),alpha);
                                if (similarLabelDst.contains(induceGraph.getEdgeTarget(edge).getType()) ) {
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
            currentQuery.getResultMatches().initialCostForTree(oGraph);
            System.out.println(currentQuery.getRoot());
            System.out.println(currentQuery.getResultMatches().classSet.size() + " " + "initial matches.");

        } else  {
            //Prune whenever it is possible.
            currentQuery.initialMatchSet(currentQuery.eid);
            HashSet<String> similarLabels = oGraph.getSimilarLabelSet(currentQuery.getRoot(),alpha);
            for (String name : similarLabels){
                System.out.println(name);
            }
            DefaultDirectedGraph<DataNode,RelationshipEdge> induceGraph =
                    induceGraphs.graphMaps.get(currentQuery.getRoot());

            HashMap<String, String> edgePairs = currentQuery.edgePairs;

            for (DataNode node : induceGraph.vertexSet()) {
                if (similarLabels.contains(node.getType())) {
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
            currentQuery.getResultMatches().initialCostForTree(oGraph);
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
                pruneByTheta(currentQuery,theta);
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

    private void pruneByTheta(OGKQuery currentQuery, double theta) {


        ArrayList<DataNode> needToRemove = new ArrayList<>();
        for (DataNode node : currentQuery.getResultMatches().classSet.keySet()) {
            double currentCost = currentQuery.getResultMatches().costMap.get(node);
            currentCost /= currentQuery.edgePairs.size();
            if (currentCost > theta) {
                needToRemove.add(node);
            } else {
                currentQuery.getResultMatches().costMap.put(node, currentCost);
            }

        }


        for (int i = 0; i < needToRemove.size(); i++) {
            currentQuery.getResultMatches().classSet.remove(needToRemove.get(i));
        }


    }

    private boolean hasIntersection(HashSet<String> similarTypesForPatternNode, HashSet<String> types) {

        if (similarTypesForPatternNode == null || types == null) {
            return false;
        }
        for (String type : types) {
            if (similarTypesForPatternNode.contains(type)) {
                return true;
            }
        }
        return false;
    }

}
