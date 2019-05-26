package DBPediaAlgorithmRunner;

import java.util.*;

import DBPediaGraph.DBPediaDataGraph;
import DBPediaOntology.DBPediaOntologyGraph;
import Infra.*;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
;
import java.util.Comparator;

public class BOGKEMQueryRunner {

    private QueriesDependencyGraph queryGraph;
    private DBPediaDataGraph dataGraph;
    private DBPediaOntologyGraph oGraph;
    private VF2AbstractIsomorphismInspector<DataNode, RelationshipEdge> inspector;
    private Comparator<DataNode> myVertexOntologyComparator;
    private Comparator<RelationshipEdge> myEdgeComparator;
    private Comparator<OGKQuery> myOGKQueryComparator;
    private DBPediaInduceGraphGenerator induceGraphs;
    private double theta;
    private int alpha;
    public static HashSet<OGKQuery> piStar = new HashSet<>();
    public static Stack<StackTriple> entriesStack = new Stack<>();

    public BOGKEMQueryRunner(QueriesDependencyGraph queryGraph, DBPediaDataGraph dataGraph, DBPediaOntologyGraph oGraph,
                             double theta, int alpha, DBPediaInduceGraphGenerator induceGraphs) {

        this.dataGraph = dataGraph;
        this.oGraph = oGraph;
        this.theta = theta;
        this.alpha = alpha;
        this.queryGraph = queryGraph;
        this.induceGraphs = induceGraphs;

        myOGKQueryComparator = new Comparator<OGKQuery>() {

            public int compare(OGKQuery o1, OGKQuery o2) {
                if (o1.ud < o2.ud) {
                    return -1;
                } else if (o1.ud > o2.ud) {
                    return 1;
                } else {
                    return 0;
                }
            }

        };

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

                    } else if (hasIntersection(o2.getSimilarTypesForPatternNode(), o1.getTypes()) || o1.getTypes().contains(o2.getNodeName())) {

                        ArrayList<String> commonLabels = new ArrayList<>();
                        for (String type : o1.getTypes()) {
                            if (o2.getSimilarTypesForPatternNode().contains(type)) {
                                commonLabels.add(type);
                            }
                        }
                        int randomIndex = randomInt(0, commonLabels.size() - 1);
                        o1.setType(commonLabels.get(randomIndex));
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

    public void execute(double budget, int b) {

        if (queryGraph.getQueries() == null || queryGraph.getQueries().size() == 0) {
            System.out.println("No Input Queries!!!");
            return;
        }


        long startTime = System.nanoTime();

        refineChase(budget, b);

        long endTime = System.nanoTime();


        System.out.println("Chase Time: " + (endTime - startTime));

        if (queryGraph.getRoot().getResultMatches() == null) {
            return;
        }
        ArrayList<HashSet<DataNode>> finalPartition = generateFinalPartition(queryGraph.getRoot().getResultMatches().
                classSet);


        ArrayList<HashSet<DataNode>> finalClasses = getFinalClasses(finalPartition,
                induceGraphs.graphMaps.get(queryGraph.getRoot().getRoot()), queryGraph.getRoot().edgePairs);


        for (int i = 0; i < finalClasses.size(); i++) {
            System.out.println("------------");
            for (DataNode node :  finalClasses.get(i)) {
                System.out.println(node.getNodeName());
                System.out.println("Theta is : " + queryGraph.getRoot().getResultMatches().costMap.get(node));
            }
            System.out.println("------------");

        }
        System.out.println(finalClasses.size() + "Total classes");

    }

    private void refineChase(double budget, int b) {

        Queue<OGKQuery> queue = new LinkedList<>();

        for (String root : queryGraph.getQueries().keySet()) {
            OGKQuery ogkQuery = queryGraph.getQueries().get(root);
            if (ogkQuery.getChildren().size() == 0 && ogkQuery.isC) {
                queue.add(ogkQuery);
            }
        }

        ArrayList<Queue<OGKQuery>> levels = new ArrayList<>();
        levels.add(new LinkedList<>(queue));

        while (queue.size() != 0) {

            int size = queue.size();
            HashSet<OGKQuery> nextLevel = new HashSet<>();

            for (int i = 0; i < size; i++) {

                OGKQuery currentQuery = queue.remove();

                if (currentQuery.getParents().size() != 0) {

                    for (OGKQuery parent : currentQuery.getParents()) {
                            nextLevel.add(parent);
                    }

                }

            }
            queue = new LinkedList<>(nextLevel);
            levels.add(new LinkedList<>(nextLevel));
        }

        ArrayList<ArrayList<OGKQuery>> lists = new ArrayList<>();

        for (int i = 0; i < levels.size(); i++) {

            ArrayList<OGKQuery> currentLists = new ArrayList<>();
            currentLists.addAll(levels.get(i));
            lists.add(new ArrayList<>(currentLists));

        }


        ArrayList<OGKQuery> leafList = new ArrayList<>();
        for (int i = 0; i < lists.get(0).size(); i++) {
            leafList.add(lists.get(0).get(i));
        }
        ArrayList<OGKQuery> nextList = new ArrayList<>();
        for (int i = 0; i < lists.get(1).size(); i++) {
            nextList.add(lists.get(1).get(i));
        }
        for (OGKQuery q : leafList) {
            System.out.print(q.getRoot() + " ");
        }
        System.out.print("\n");

        for (OGKQuery q : nextList) {
            System.out.print(q.getRoot() + " ");
        }
        System.out.print("\n");

        HashSet<OGKQuery> pi = new HashSet<>();


        dfsRefine(leafList, nextList, pi, b,budget, 0, lists);
        pruneByTheta(queryGraph.getRoot(),theta);

        MatchResult result = queryGraph.getRoot().getResultMatches();
        System.out.print(queryGraph.getRoot().getRoot() + " ");
        if (result == null) {
            System.out.println("Empty Classes!");
            return;
        }

        System.out.println(result.classSet.size() + " Classes.");

//        for (DataNode dataNode : result.classSet.keySet()) {
//            HashMap<Integer, Integer> eids = result.classSet.get(dataNode);
//            System.out.println("Name : " + dataNode.getNodeName());
//            for (Integer eid : eids.keySet()) {
//                System.out.print("Eid " + eid + " : ");
//                System.out.println(eids.get(eid));
//            }
//            System.out.println("--------");
//        }
//
//        for (OGKQuery refinedQuery : piStar) {
//            System.out.println(refinedQuery.getRoot() + " " + refinedQuery.ud);
//        }


    }

    private void dfsRefine(ArrayList<OGKQuery> openList, ArrayList<OGKQuery> nextList, HashSet<OGKQuery> pi,
                           int b, double budget, int level, ArrayList<ArrayList<OGKQuery>> lists) {

        if (budget < 0 || (openList.size() == 0 && nextList.size() == 0)) {

            if (budget < 0) {
                System.out.println("outofBudget!");
            }
            double piC = 0;
            double piStarC = 0;
            for (OGKQuery query : pi) {
                piC += query.ud;
            }
            for (OGKQuery query : piStar) {
                piStarC += query.ud;
            }
            if (piStarC > piC) {
                piStar = new HashSet<>(pi);
            }
            System.out.println(piStarC);
            System.out.println("DFS Terminated!!!");

            return;

        }

        System.out.print("current budget!!!! " + budget);
        for (OGKQuery q : openList) {
            System.out.print(q.getRoot() + " 1");

        }
        System.out.print("\n");
        for (OGKQuery q : nextList) {
            System.out.print(q.getRoot() + " 2");

        }

        entriesStack.push(new StackTriple(level, 0, budget));

        for (OGKQuery query : openList) {

            if (query.isC) {
                getInitialMatches(query);
                pruneByTheta(query, theta);
                query.getResultMatches().setMaxCost();
                query.ud = query.getResultMatches().maxCost;

            } else {
                getInitialMatches(query);
                pruneByTheta(query, theta);
                query.getResultMatches().setMaxCost();
                query.ud = query.getResultMatches().maxCost;

            }

        }

        for (OGKQuery q : openList) {
            System.out.print(q.getRoot() + " before" + q.ud);

        }

        Collections.sort(openList, myOGKQueryComparator);

        System.out.print("After sort:");
        for (OGKQuery q : openList) {
            System.out.print(q.getRoot() + " ");
        }
        System.out.print("\n");


        HashSet<OGKQuery> lSet = getLSet(openList, b, entriesStack);

        for (OGKQuery q : lSet) {
            System.out.print(q.getRoot() + " final Set");
        }
        System.out.print("\n");

        while (lSet.size() != 0) {

            for (OGKQuery query : lSet) {

                if (query.isC) {
                    query.getResultMatches().partitionResultSets(query.getResultMatches().matchesMap);
                } else {

                    for (OGKQuery child : query.getChildren()) {
                        query.getResultMatches().chaseByChild(child.getResultMatches(),
                                induceGraphs.graphMaps.get(query.getRoot()));
                    }

                }

                pi.add(query);
                query.isVal = true;
                double total = 0.0;

                for (DataNode dataNode : query.getResultMatches().classSet.keySet()) {
                    total += query.getResultMatches().costMap.get(dataNode);
                    entriesStack.peek().higherBound -= query.getResultMatches().costMap.get(dataNode);
                    if (entriesStack.peek().higherBound < 0) {
                        System.out.println(entriesStack.peek().higherBound + "left suddenly terminated!!!");
                        return;
                    }
                }
                openList.remove(query);
                System.out.print(total + " total cost for" + query.getRoot());

            }

            ArrayList<OGKQuery> nextOpenList = new ArrayList();

            for (OGKQuery ogkquery : nextList) {

                boolean canBeAdded = true;

                for (OGKQuery child :ogkquery.getChildren()) {
                    if (!child.isVal) {
                        canBeAdded = false;
                        break;
                    }
                }
                if(canBeAdded) {
                    nextOpenList.add(ogkquery);
                }

            }
            for (OGKQuery q : nextOpenList) {
                System.out.print(q.getRoot() + " !!!!");
            }
            System.out.print("\n");

            ArrayList<OGKQuery> nextNextOpenList = new ArrayList();
            if ((level + 2) <= (lists.size() - 1)) {
                nextNextOpenList = lists.get(level + 2);
            }


            dfsRefine(nextOpenList, nextNextOpenList, pi, b, entriesStack.peek().higherBound, level+1, lists);
            lSet = getLSet(openList, b, entriesStack);
            System.out.println("nextSize" + lSet.size());

        }

        entriesStack.pop();

    }

    private HashSet<OGKQuery> getLSet(ArrayList<OGKQuery> openList, int b, Stack<StackTriple> entriesStack) {


        if (entriesStack.size() == 0 || openList.size() == 0) {
            if(entriesStack.size()  == 0) {
                System.out.print("none");
            }
            return new HashSet<>();
        }
        double maxUvd = 0.0;
        HashSet<OGKQuery> lSet = new HashSet<>();
        for (int i = 0; i < openList.size(); i++){
            OGKQuery query = openList.get(i);
            if (query.ud <= entriesStack.peek().higherBound && query.ud >= entriesStack.peek().lowerBound) {

                lSet.add(query);
//                System.out.println(query.getRoot() + "added");
//                System.out.println(entriesStack.peek().lowerBound + " to " + entriesStack.peek().higherBound);

                if (maxUvd < query.ud) {
                    maxUvd = query.ud;
                }

                if (lSet.size() == b) {
                    break;
                }
            }
        }
        entriesStack.peek().lowerBound = maxUvd;
        System.out.println(entriesStack.peek().lowerBound + " to " + entriesStack.peek().higherBound);

        return new HashSet(lSet);

    }

    private boolean isEmbedding(ArrayList<DataNode> currentMatch, ArrayList<DataNode> patternTypes, double theta,
                                DBPediaOntologyGraph oGraph, int rootIndex, OGKQuery ogkQuery) {

        double decayFactor = 0.9; // tunable
        double vPSize = patternTypes.size();
        double sumCr = 0.0;


        for (int i = 0; i < currentMatch.size(); i++) {

            if (currentMatch.get(i).getNodeType() == NodeType.Literal) {
                continue;
            } // Literal Nodes Cost are all zero, cause are all exact matches!

            if (ogkQuery.getParents().size() != 0) {

                if (i == rootIndex) {

                    for (int j = 0; j < ogkQuery.getParents().size(); j++) {

                        HashMap<DataNode, Double> parentCostMap = ogkQuery.getParents().get(j).
                                getResultMatches().costMap;
                        if (parentCostMap.containsKey(currentMatch.get(i))) {
                            double newCost = (decayFactor * (parentCostMap.get(currentMatch.get(i))));
                            sumCr += newCost;

                            break;
                        }
                        System.out.println("SubRoot Detected!");
                    }


                } else {

                    if (oGraph.distMap.containsKey(patternTypes.get(i).getNodeName())) {

                        double dist = decayFactor * oGraph.distMap.get(patternTypes.get(i).
                                getNodeName()).get(currentMatch.get(i).getType());
                        double newCost = dist * decayFactor * 0.1; // = *1
                        sumCr += newCost;

                    } else {
                        System.out.println("No cost dist found!");
                    }

                }

            } else {

                if (oGraph.distMap.containsKey(patternTypes.get(i).getNodeName())) {

                    double dist = decayFactor * oGraph.distMap.get(patternTypes.get(i).
                            getNodeName()).get(currentMatch.get(i).getType());
                    double newCost;

                    if (i == rootIndex) {
                        newCost = dist * 0.1;
                    } else {
                        newCost = dist * decayFactor * 0.1;//* assuming decay actor all edges are one hop
                    }

                    sumCr += newCost;

                } else {
                    System.out.println("No cost dist found!");
                }

            }

        }

        sumCr /= vPSize;

        for (int i = 0; i < currentMatch.size(); i++) {
            if (!ogkQuery.getResultMatches().costMap.containsKey(currentMatch.get(i))) {
                ogkQuery.getResultMatches().costMap.put(currentMatch.get(i), sumCr);
            }
        }

        System.out.println("Theta is " + sumCr + " !");

        if (sumCr < theta) {
            return true;
        }

        return false;
    }

    private void getInitialMatches(OGKQuery currentQuery) {

        System.out.println(currentQuery.getRoot());
        System.out.println(currentQuery.isC + "Check!");
        if (!currentQuery.isC) {

            currentQuery.initialMatchSet(currentQuery.eid);
            HashSet<String> similarLabelSrc = oGraph.getSimilarLabelSet(currentQuery.getRoot(), alpha);
            HashMap<String, String> edgePairs = currentQuery.edgePairs;


            DefaultDirectedGraph<DataNode, RelationshipEdge> induceGraph = induceGraphs.graphMaps.get(currentQuery.getRoot());


            for (DataNode dataNode : induceGraph.vertexSet()) {

                if (hasIntersection(similarLabelSrc,dataNode.getTypes())) {
                    boolean isValidCandidate = true;
                    HashMap<String, Boolean> factors = new HashMap();
                    for (String key : edgePairs.keySet()) {
                        factors.put(key, false);
                    }
                    for (RelationshipEdge edge : induceGraph.outgoingEdgesOf(dataNode)) {
                        if (edgePairs.containsKey(edge.getLabel())) {
                            if (edgePairs.get(edge.getLabel()).equals(edge.getLabel())) {
                                if (induceGraph.getEdgeTarget(edge).getNodeType() == NodeType.Literal) {
                                    factors.put(edge.getLabel(), true);
                                }
                            } else {
                                HashSet<String> similarLabelDst = oGraph.getSimilarLabelSet(edgePairs.get(edge.getLabel()), alpha);
                                if (similarLabelDst.contains(induceGraph.getEdgeTarget(edge).getType())) {
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

        } else {

            currentQuery.initialMatchSet(currentQuery.eid);
            HashSet<String> similarLabels = oGraph.getSimilarLabelSet(currentQuery.getRoot(), alpha);

            DefaultDirectedGraph<DataNode, RelationshipEdge> induceGraph = induceGraphs.graphMaps.get(currentQuery.getRoot());

            HashMap<String, String> edgePairs = currentQuery.edgePairs;

            for (DataNode node : induceGraph.vertexSet()) {
                if (hasIntersection(similarLabels,node.getTypes())) {
                    currentQuery.getResultMatches().classSet.put(node, new HashMap<Integer, Integer>());
                }
            }
            HashMap<DataNode, String> matches = new HashMap<>();
            ArrayList<DataNode> needToRemove = new ArrayList<>();

            for (String edgePredicate : edgePairs.keySet()) {
                for (DataNode candidate : currentQuery.getResultMatches().classSet.keySet()) {
                    boolean canBePruned = true;
                    for (RelationshipEdge edge : induceGraph.outgoingEdgesOf(candidate)) {
                        if (edge.getLabel().equals(edgePredicate) && induceGraph.getEdgeTarget(edge).getNodeType() == NodeType.Literal) {
                            canBePruned = false;
                            matches.put(candidate, induceGraph.getEdgeTarget(edge).getNodeName());
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

            }
            currentQuery.getResultMatches().matchesMap = new HashMap<>(matches);
            currentQuery.getResultMatches().initialCostForTree(oGraph);
            System.out.println(currentQuery.getRoot());
            System.out.println(currentQuery.getResultMatches().classSet.size() + " " + "initial matches.");

        }
    }

    private void pruneByTheta(OGKQuery currentQuery, double theta) {

        if (currentQuery.getResultMatches() == null
                || currentQuery.getResultMatches().classSet.size() == 0){
            return;
        }

        ArrayList<DataNode> needToRemove = new ArrayList<>();
        for (DataNode node : currentQuery.getResultMatches().classSet.keySet()) {
            double currentCost = currentQuery.getResultMatches().costMap.get(node);
            currentCost /= currentQuery.edgePairs.size();
            if (currentCost > theta) {
                needToRemove.add(node);
            } else {
                currentQuery.getResultMatches().costMap.put(node, currentCost);
                //System.out.println("cost updated" + currentCost);
            }

        }


        for (int i = 0; i < needToRemove.size(); i++) {
            currentQuery.getResultMatches().classSet.remove(needToRemove.get(i));
        }


    }

    public static int randomInt(int low, int high) {
        return (int) (Math.random() * (high - low)) + low;
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
    private ArrayList<HashSet<DataNode>> getFinalClasses(ArrayList<HashSet<DataNode>> finalPartition,
                                                         DefaultDirectedGraph<DataNode, RelationshipEdge> induceGraph,
                                                         HashMap<String, String> edgePairs) {


        ArrayList<HashSet<DataNode>> finalClasses = new ArrayList<>();
        if (finalPartition == null) {
            return finalClasses;

        }
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
        if (classSet == null) {
            return classes;
        }

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


}
