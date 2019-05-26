package DBPediaAlgorithmRunner;

import java.util.*;

import DBPediaGraph.DBPediaDataGraph;
import DBPediaOntology.DBPediaOntologyGraph;
import Infra.*;
import Util.DBPediaVF2PatternConverter;
import org.jgrapht.Graph;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
;
import java.util.Comparator;

public class BEnumQueryRunner {

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

    public BEnumQueryRunner(QueriesDependencyGraph queryGraph, DBPediaDataGraph dataGraph, DBPediaOntologyGraph oGraph,
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



        System.out.println("Final Partition:");
//        int total = 0;
//        for (int i = 0; i < finalPartition.size(); i++) {
//            total += finalPartition.get(i).size();
//            System.out.println("------------");
//            for (DataNode node :  finalPartition.get(i)) {
//                System.out.println(node.getNodeName());
//            }
//            System.out.println("------------");
//
//        }
        System.out.println(finalPartition.size() + "Total");

        ArrayList<HashSet<DataNode>> finalClasses = getFinalClasses(finalPartition,
                induceGraphs.graphMaps.get(queryGraph.getRoot().getRoot()), queryGraph.getRoot().edgePairs);

//        System.out.println(finalClasses.size());
//        total = 0;
//        for (int i = 0; i < finalClasses.size(); i++) {
//            total += finalClasses.get(i).size();
//            System.out.println("------------");
//            for (DataNode node :  finalClasses.get(i)) {
//                System.out.println(node.getNodeName());
//                System.out.println("Theta is : " + queryGraph.getRoot().getResultMatches().costMap.get(node));
//            }
//            System.out.println("------------");
//
//        }
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
        // all levels queue


        ArrayList<ArrayList<OGKQuery>> lists = new ArrayList<>();

        for (int i = 0; i < levels.size(); i++) {

            ArrayList<OGKQuery> currentLists = new ArrayList<>();
            currentLists.addAll(levels.get(i));
            lists.add(new ArrayList<>(currentLists));

        }//all levels


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


        DFSRefine(leafList, nextList, pi, b,budget, 0, lists);


        MatchResult result = queryGraph.getRoot().getResultMatches();
        System.out.print(queryGraph.getRoot().getRoot() + " ");
        if (result == null) {
            System.out.println("Empty Classes!");
            return;
        }

//        System.out.println(result.classSet.size() + " Classes.");
//
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

    private void DFSRefine(ArrayList<OGKQuery> openList, ArrayList<OGKQuery> nextList, HashSet<OGKQuery> pi,
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
                query.getResultMatches().setMaxCost();
                query.ud = query.getResultMatches().maxCost;

            } else {
                getInitialMatches(query);
                updateCost(query);
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
                    query.getResultMatches().classifyResultSets();
                } else {

                    for (OGKQuery childQuery : query.getChildren()) {
                        query.getResultMatches().chaseByChildEnum(childQuery.getResultMatches(),
                                queryGraph,induceGraphs,query.getRoot(),childQuery.getRoot());
                    }

                }

                pi.add(query);
                query.isVal = true;
                double totalCost = 0.0;
                for (DataNode dataNode : query.getResultMatches().classSet.keySet()) {
                    totalCost += query.getResultMatches().costMap.get(dataNode);
                }

                entriesStack.peek().higherBound -= totalCost;
                System.out.println(entriesStack.peek().higherBound + "left");

                openList.remove(query);

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
                System.out.println(canBeAdded + "Boolean");
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


            DFSRefine(nextOpenList, nextNextOpenList, pi, b, entriesStack.peek().higherBound, level+1, lists);
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
                System.out.println(entriesStack.peek().lowerBound + " to " + entriesStack.peek().higherBound);

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


    private void ontologySearch(OGKQuery ogkQuery) {

        Graph<DataNode, RelationshipEdge> pattern = new DBPediaVF2PatternConverter(ogkQuery, oGraph, alpha).getPattern();
        Graph<DataNode, RelationshipEdge> inducedGraph = induceGraphs.graphMaps.get(ogkQuery.getRoot());
        System.out.println("Nodes Size :" + inducedGraph.vertexSet().size());
        System.out.println("Graph Size :" + inducedGraph.edgeSet().size());
        inspector = new VF2SubgraphIsomorphismInspector<>(inducedGraph, pattern,
                myVertexOntologyComparator, myEdgeComparator, false);

        if (inspector.isomorphismExists()) {

            Iterator<GraphMapping<DataNode, RelationshipEdge>> iterator = inspector.getMappings();
            ArrayList<DataNode> patternTypes = new ArrayList<>();

            for (DataNode node : pattern.vertexSet()) {
                patternTypes.add(node);
            }


            ogkQuery.initialMatchSet(ogkQuery.eid);

            ArrayList<ArrayList<DataNode>> matches = new ArrayList<>();

            while (iterator.hasNext()) {

                org.jgrapht.GraphMapping<DataNode, RelationshipEdge> mappings = iterator.next();
                ArrayList<DataNode> currentMatch = new ArrayList<>();
                for (DataNode node : pattern.vertexSet()) {

                    DataNode currentMatchedNode = mappings.getVertexCorrespondence(node, false);
                    if (currentMatchedNode != null) {

                        currentMatch.add(currentMatchedNode);
                    }

                }
                matches.add(new ArrayList<>(currentMatch));
                if(ogkQuery.isC)  {
                    //System.out.println("One match added! for C tree!");
                } else {
                    //System.out.println("One match added! for V tree!");
                }


            }

            ogkQuery.getResultMatches().matches = matches;
            ogkQuery.getResultMatches().classifyResultSets();
            System.out.println("Current Matches for" + ogkQuery.getRoot() + " " +  + matches.size());
        } else {

            System.out.println("No Matches for the query!");

        }

    }



    private void getInitialMatches(OGKQuery currentQuery) {

        System.out.println(currentQuery.isC + "check");
        if (!currentQuery.isC) {

            currentQuery.initialMatchSet(currentQuery.eid);
            HashSet<String> similarLabels = oGraph.getSimilarLabelSet(currentQuery.getRoot(),alpha);

            for (DataNode dataNode : induceGraphs.graphMaps.get(currentQuery.getRoot()).vertexSet()) {
                if (similarLabels.contains(dataNode.getType())) {
                    currentQuery.getResultMatches().classSet.put(dataNode, new HashMap<Integer, Integer>());
                }
            }
            currentQuery.getResultMatches().initialCostForTree(oGraph);
            System.out.println(currentQuery.getRoot());
            System.out.println(currentQuery.getResultMatches().classSet.size() + " " + "initial matches.");


        } else {

            long start = System.nanoTime();
            ontologySearch(currentQuery);
            long current = System.nanoTime();
            System.out.println(current - start + "s for VF2!!!!!!" );
            currentQuery.getResultMatches().initialCostForTree(oGraph);
            System.out.println(currentQuery.getResultMatches().classSet.size() + " " + "initial matches.");

        }

    }

    private void updateCost(OGKQuery currentQuery) {


        for (DataNode node : currentQuery.getResultMatches().classSet.keySet()) {
            double currentCost = currentQuery.getResultMatches().costMap.get(node);
            currentCost /= currentQuery.edgePairs.size();
            currentQuery.getResultMatches().costMap.put(node, currentCost);

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
            return finalPartition;
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
