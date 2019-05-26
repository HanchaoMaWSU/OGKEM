package DBPediaAlgorithmRunner;

import java.util.*;
import DBPediaOntology.DBPediaOntologyGraph;
import Infra.*;
import Util.DBPediaVF2PatternConverter;
import org.jgrapht.Graph;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.GraphMapping;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.Comparator;

public class EnumEMQueryRunner {

    private QueriesDependencyGraph queryGraph;
    private DBPediaOntologyGraph oGraph;
    private VF2AbstractIsomorphismInspector<DataNode, RelationshipEdge> inspector;
    private Comparator<DataNode> myVertexOntologyComparator;
    private Comparator<RelationshipEdge> myEdgeComparator;
    DBPediaInduceGraphGenerator induceGraphs;
    private int alpha;

    public EnumEMQueryRunner(QueriesDependencyGraph queryGraph, DBPediaOntologyGraph oGraph,
                             int alpha, DBPediaInduceGraphGenerator induceGraphs) {

        this.oGraph = oGraph;
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

                    if (o2.getNodeType() == NodeType.Literal
                            && o1.getNodeType() == NodeType.Literal) {

                        return 0;

                    } else if (hasIntersection(o2.getSimilarTypesForPatternNode(),o1.getTypes())) {
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

        Queue<OGKQuery> currentLevel = new LinkedList<>();

        for (String root : queryGraph.getQueries().keySet())  {
            OGKQuery query = queryGraph.getQueries().get(root);
            if (query.isC() && query.getChildren().size() == 0) {
                currentLevel.add(query);
            }
        }

        while (currentLevel.size() != 0) {

            int size = currentLevel.size();

            for (int i =  0; i < size; i++) {

                OGKQuery ogkQuery = currentLevel.remove();

                if (ogkQuery.isC) {
                    getInitialMatches(ogkQuery);
                    ogkQuery.getResultMatches().classifyResultSets();

                } else {
                    getInitialMatches(ogkQuery);
                    if (ogkQuery.getChildren().size() != 0){
                        for (OGKQuery childQuery : ogkQuery.getChildren()) {
                            ogkQuery.getResultMatches().chaseByChildEnum(childQuery.getResultMatches(),
                                    queryGraph,induceGraphs,ogkQuery.getRoot(),childQuery.getRoot());

                        }


                    }


                }

                for (OGKQuery parent : ogkQuery.getParents()) {
                    currentLevel.add(parent);
                }

            }

        }

        MatchResult result = queryGraph.getRoot().getResultMatches();
        System.out.print(queryGraph.getRoot().getRoot() + " ");
        if (result == null) {
            System.out.println("Empty Classes!");
            return;
        }

        ArrayList<HashSet<DataNode>> finalPartition = generateFinalPartition(queryGraph.getRoot().getResultMatches().
                classSet);
        ArrayList<HashSet<DataNode>> finalClasses = getFinalClasses(finalPartition,
                induceGraphs.graphMaps.get(queryGraph.getRoot().getRoot()), queryGraph.getRoot().edgePairs);

        System.out.println(finalClasses.size());
        int total = 0;
        for (int i = 0; i < finalClasses.size(); i++) {
            total += finalClasses.get(i).size();
            System.out.println("------------");
            for (DataNode node :  finalClasses.get(i)) {
                System.out.println(node.getNodeName());
            }
            System.out.println("------------");

        }
        System.out.println(total + "Total classes");

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

        System.out.println(currentQuery.isC);
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

    private void ontologySearch(OGKQuery ogkQuery) {

        Graph<DataNode, RelationshipEdge> pattern = new
                DBPediaVF2PatternConverter(ogkQuery, oGraph, alpha).getPattern();
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

            }

            ogkQuery.getResultMatches().matches = matches;
            System.out.println("Current Matches for" + ogkQuery.getRoot() + " " +  + matches.size());
        } else {

            System.out.println("No Matches for the query!");

        }

    }


    private boolean hasIntersection(HashSet<String> similarTypesForPatternNode,
                                    HashSet<String> types) {

        for (String type : types) {
            if (similarTypesForPatternNode.contains(type)) {
                return true;
            }
        }
        return false;
    }

}
