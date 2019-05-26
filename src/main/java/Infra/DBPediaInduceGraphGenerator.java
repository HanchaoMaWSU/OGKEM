package Infra;

import java.util.*;

import DBPediaGraph.DBPediaDataGraph;
import DBPediaOntology.DBPediaOntologyGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import javax.xml.soap.Node;


public class DBPediaInduceGraphGenerator {

    private QueriesDependencyGraph queryGraph;
    private DBPediaDataGraph dataGraph;
    private DBPediaOntologyGraph oGraph;
    private int alpha;
    public HashMap<String, DefaultDirectedGraph<DataNode,RelationshipEdge>> graphMaps;
    private DefaultDirectedGraph<DataNode,RelationshipEdge> sizedGraph;

    public DBPediaInduceGraphGenerator(QueriesDependencyGraph queryGraph, DBPediaDataGraph dataGraph, DBPediaOntologyGraph oGraph, int alpha) {
        this.dataGraph = dataGraph;
        this.oGraph = oGraph;
        this.alpha = alpha;
        this.queryGraph = queryGraph;
        this.graphMaps = new HashMap<>();
    }

    public void generateInduceGraphs() {

        for (String queryRoot : queryGraph.getQueries().keySet()) {
            OGKQuery currentQuery = queryGraph.getQueries().get(queryRoot);
            DefaultDirectedGraph iGraph = getRandomIGraph(currentQuery, dataGraph.getDataGraph());
            this.graphMaps.put(currentQuery.getRoot(), iGraph);
        }

    }

    public void setSizedGraph (int size) {
        this.sizedGraph = getGraphBySize(size);
    }

    public void generateSizedGraphs() {


        for (String queryRoot : queryGraph.getQueries().keySet()) {
            OGKQuery currentQuery = queryGraph.getQueries().get(queryRoot);
            DefaultDirectedGraph iGraph = getRandomIGraph(currentQuery, sizedGraph);
            this.graphMaps.put(currentQuery.getRoot(), iGraph);
            System.out.println(graphMaps.get(queryRoot).vertexSet().size() + " "
                    + graphMaps.get(queryRoot).edgeSet().size());
        }

    }




    private DefaultDirectedGraph<DataNode, RelationshipEdge> getRandomIGraph(OGKQuery ogkQuery,
                                                                             Graph<DataNode,
                                                                                     RelationshipEdge> dataGraph) {

        HashSet<String> predicateMap = new HashSet();
        for (OGKQueryEdge edge : ogkQuery.getQueryEdgeList()) {
            predicateMap.add(edge.getEdgePredicate());
        }
        DefaultDirectedGraph<DataNode, RelationshipEdge> iGraph = generateRandomInducedGraph(ogkQuery,
                predicateMap, oGraph, dataGraph);

        return iGraph;
    }

    private DefaultDirectedGraph<DataNode, RelationshipEdge>
    generateRandomInducedGraph(OGKQuery query, HashSet<String> predicateSet,
                               DBPediaOntologyGraph oGraph, Graph<DataNode, RelationshipEdge> dataGraph) {

        DefaultDirectedGraph iGraph = new DefaultDirectedGraph<DataNode, RelationshipEdge>(RelationshipEdge.class);
        HashSet<DataNode> induceNodes = new HashSet<>();
        HashSet<String> labelSet = new HashSet<>();

        int count = 0;
        HashSet<String> similarLabels = oGraph.getSimilarLabelSet(query.getRoot(), alpha);

        if (similarLabels != null) {
            labelSet.addAll(similarLabels);
        }

        for (DataNode dataNode : dataGraph.vertexSet()) {

            if (dataNode.getNodeType() == NodeType.Literal) {
                continue;
            }

            if (!checkLiterals(dataGraph, dataNode, query.getLiterals())) {
                continue;
            }

            ArrayList<String> commonLabels = getIntersection(labelSet, dataNode.getTypes());
            if (commonLabels.size() > 0) {

                iGraph.addVertex(dataNode);
                induceNodes.add(dataNode);

            }

        }

        System.out.println(count + "Nodes !!!!!!!");

        for (DataNode node : induceNodes) {

            for (RelationshipEdge edge : dataGraph.outgoingEdgesOf(node)) {

                DataNode src = dataGraph.getEdgeSource(edge);
                DataNode dst = dataGraph.getEdgeTarget(edge);
                iGraph.addVertex(src);
                iGraph.addVertex(dst);

                if (predicateSet.contains(edge.getLabel())) {
                    iGraph.addEdge(src, dst, edge);
                }

            }

            for (RelationshipEdge edge : dataGraph.incomingEdgesOf(node)) {

                DataNode src = dataGraph.getEdgeSource(edge);
                DataNode dst = dataGraph.getEdgeTarget(edge);

                iGraph.addVertex(src);
                iGraph.addVertex(dst);
                if ((predicateSet.contains(edge.getLabel()))) {
                    iGraph.addEdge(src, dst, edge);
                }

            }

        }

        return iGraph;
    }

    private boolean checkLiterals(Graph<DataNode, RelationshipEdge> dataGraph, DataNode dataNode,
                                  HashMap<String, String> literals) {

             if (literals.size() == 0) {
                 return true;
             }

             boolean check = true;
             for (String literal: literals.keySet()) {

                 for (RelationshipEdge edge: dataGraph.outgoingEdgesOf(dataNode)) {
                     if (edge.getLabel().equals(literal)) {
                         check = check && (dataGraph.getEdgeTarget(edge).getNodeName().equals(literals.get(literal)));
                     }
                 }

             }

             return check;

    }

    private ArrayList<String> getIntersection(HashSet<String> similarLabel, HashSet<String> types) {

        ArrayList<String> commonLabels = new ArrayList<>();

        for (String label : similarLabel) {
            if (types.contains(label)) {
                commonLabels.add(label);
            }
        }
        return commonLabels;
    }

    private DefaultDirectedGraph<DataNode,RelationshipEdge> getGraphBySize(int size) {

        DefaultDirectedGraph sizedGraph = new DefaultDirectedGraph<DataNode, RelationshipEdge>(RelationshipEdge.class);

        for (DataNode dataNode : dataGraph.getDataGraph().vertexSet()) {



            sizedGraph.addVertex(dataNode);

            for (RelationshipEdge edge : dataGraph.getDataGraph().outgoingEdgesOf(dataNode)) {

                DataNode src = dataGraph.getDataGraph().getEdgeSource(edge);
                DataNode dst = dataGraph.getDataGraph().getEdgeTarget(edge);
                sizedGraph.addVertex(src);
                sizedGraph.addVertex(dst);
                sizedGraph.addEdge(src, dst, edge);
            }
            for (RelationshipEdge edge : dataGraph.getDataGraph().incomingEdgesOf(dataNode)) {

                DataNode src = dataGraph.getDataGraph().getEdgeSource(edge);
                DataNode dst = dataGraph.getDataGraph().getEdgeTarget(edge);
                sizedGraph.addVertex(src);
                sizedGraph.addVertex(dst);
                sizedGraph.addEdge(src, dst, edge);
            }

            if(sizedGraph.edgeSet().size() > size) {
                break;
            }

        }

        System.out.println(sizedGraph.edgeSet().size() + "new edges");
        System.out.println(sizedGraph.vertexSet().size() + "new nodes");

        for (String key : graphMaps.keySet()) {

            DefaultDirectedGraph<DataNode, RelationshipEdge> currentGraph = graphMaps.get(key);
            for (DataNode node : currentGraph.vertexSet()) {
                sizedGraph.addVertex(node);
                for (RelationshipEdge edge : currentGraph.outgoingEdgesOf(node)) {

                    DataNode src = currentGraph.getEdgeSource(edge);
                    DataNode dst = currentGraph.getEdgeTarget(edge);
                    sizedGraph.addVertex(src);
                    sizedGraph.addVertex(dst);
                    sizedGraph.addEdge(src, dst, edge);

                }
                for (RelationshipEdge edge : currentGraph.incomingEdgesOf(node)) {

                    DataNode src = currentGraph.getEdgeSource(edge);
                    DataNode dst = currentGraph.getEdgeTarget(edge);
                    sizedGraph.addVertex(src);
                    sizedGraph.addVertex(dst);
                    sizedGraph.addEdge(src, dst, edge);
                }
            }


        }

        System.out.println(sizedGraph.edgeSet().size() + "new edges after include match");
        System.out.println(sizedGraph.vertexSet().size() + "new nodes after include match");

        return sizedGraph;
    }
}
