package Util;

import DBPediaOntology.DBPediaOntologyGraph;
import Infra.*;
import YagoOntology.YagoOntologyGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class YagoVF2PatternConverter {

    private Graph<DataNode,RelationshipEdge> pattern;
    private  HashMap<String, OGKQueryNode> nodeMap;
    YagoOntologyGraph ontology;

    public YagoVF2PatternConverter(OGKQuery query,
                                   YagoOntologyGraph ontology, int alpha) {

        this.ontology = ontology;
        this.pattern = new DefaultDirectedGraph<>(RelationshipEdge.class);
        this.nodeMap = query.getQueryNodeMap();
        convertQueryToPattern(query,alpha);

    }

    private void convertQueryToPattern(OGKQuery query, int alpha) {

        ArrayList<OGKQueryEdge> queryEdges = query.getQueryEdgeList();

        HashSet<String> duplicates = new HashSet<>();
        HashMap<String, DataNode> queryNodeUpdatedMap = new HashMap<>();

        for (OGKQueryEdge queryEdge: queryEdges) {

            if (!duplicates.contains(queryEdge.getSrc())) {
                OGKQueryNode currentNode = nodeMap.get(queryEdge.getSrc());
                DataNode currentPatternNode = new DataNode(currentNode.getTypeString(),
                        currentNode.getNodeType(), "Pattern");

                HashSet<String> newTypes = ontology.getSimilarLabelSet(currentPatternNode.getNodeName(),
                        alpha);

                if (newTypes != null) {
                    for (String type : newTypes) {
                        currentPatternNode.addSimilarTypes(type);
                    }
                }
                currentPatternNode.addSimilarTypes(currentPatternNode.getNodeName());

                pattern.addVertex(currentPatternNode);
                queryNodeUpdatedMap.put(queryEdge.getSrc(), currentPatternNode);
                duplicates.add(queryEdge.getSrc());
            }

            if (!duplicates.contains(queryEdge.getDst())) {

                OGKQueryNode currentNode = nodeMap.get(queryEdge.getDst());

                DataNode currentPatternNode = new DataNode(currentNode.getTypeString(),
                        currentNode.getNodeType(), "Pattern");


                if (currentNode.getNodeType() != NodeType.Literal) {

                     HashSet<String> newTypes = ontology.
                             getSimilarLabelSet(currentPatternNode.getNodeName(),alpha);
                     if (newTypes != null) {
                         for (String type : newTypes) {
                             currentPatternNode.addSimilarTypes(type);
                         }
                     }

                }

                currentPatternNode.addSimilarTypes(currentPatternNode.getNodeName());
                pattern.addVertex(currentPatternNode);
                queryNodeUpdatedMap.put(queryEdge.getDst(), currentPatternNode);
                duplicates.add(queryEdge.getDst());

            }

            RelationshipEdge currentPatternEdge =
                    new RelationshipEdge(queryEdge.getEdgePredicate());
            pattern.addEdge(queryNodeUpdatedMap.get(queryEdge.getSrc()),
                    queryNodeUpdatedMap.get(queryEdge.getDst()), currentPatternEdge);

        }

    }

    public Graph<DataNode, RelationshipEdge> getPattern() {
        return pattern;
    }

}
