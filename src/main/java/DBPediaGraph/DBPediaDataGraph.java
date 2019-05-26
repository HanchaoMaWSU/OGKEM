package DBPediaGraph;

import Infra.DataGraphBase;
import Infra.DataNode;
import Infra.NodeType;
import Infra.RelationshipEdge;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.rdf.model.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DBPediaDataGraph extends DataGraphBase {

    public DBPediaDataGraph(String nodeTypesFilePath, String dataGraphFilePath) {
        super();
        loadNodeMap(nodeTypesFilePath);
        addAllVertex();
        loadGraph(dataGraphFilePath);
    }

    public void mergeOtherGraph (String dataGraphFilePath, String typeFilePath) {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }

        loadNodeMap(typeFilePath);
        Model model = ModelFactory.createDefaultModel();
        System.out.println("Merging Another DBPedia Graph...");
        model.read(dataGraphFilePath);
        StmtIterator dataTriples = model.listStatements();

        while (dataTriples.hasNext()) {

            Statement stmt = dataTriples.nextStatement();
            String subject = stmt.getSubject().getURI().toLowerCase();

            if (subject.length() > 28) {
                subject = subject.substring(28);
            }

            String predicate = stmt.getPredicate().getLocalName().toLowerCase();
            RDFNode object = stmt.getObject();
            String objectString;

            try {

                if (object.isLiteral()) {
                    objectString = object.asLiteral().getString();
                } else {
                    objectString = object.asResource().getLocalName();
                }

            } catch (DatatypeFormatException e) {
                System.out.println("Invalid DataType Skipped!");
                e.printStackTrace();
                continue;
            }

            int subjectNodeId = subject.hashCode();

            if (!nodeMap.containsKey(subjectNodeId)) {
                nodeMap.put(subjectNodeId, new DataNode(subject, NodeType.Entity, "Entity"));
            }

            int objectNodeId = objectString.hashCode();

            DataNode currentSubject = nodeMap.get(subjectNodeId);
            this.dataGraph.addVertex(currentSubject);

            DataNode currentObject;

            if (object.isURIResource()) {

                if (subjectNodeId == objectNodeId) {
                    continue;
                }

                if (!nodeMap.containsKey(objectNodeId)) {
                    nodeMap.put(objectNodeId, new DataNode(objectString, NodeType.Entity, "Entity"));

                }

                currentObject = nodeMap.get(objectNodeId);
                this.dataGraph.addVertex(currentObject);

            } else if (object.isLiteral()) {

                currentObject = new DataNode(objectString, NodeType.Literal, "Literal");
                this.dataGraph.addVertex(currentObject);

            } else {

                continue;

            }


            dataGraph.addEdge(currentSubject, currentObject, new RelationshipEdge(predicate));


        }

        System.out.println("Number of Edges: " + dataGraph.edgeSet().size());
        System.out.println("Number of Nodes: " + dataGraph.vertexSet().size());
        System.out.println("Done Loading DBPedia Graph!");

    }

    private void loadNodeMap(String nodeTypesFilePath) {

        if (nodeTypesFilePath == null || nodeTypesFilePath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }
        System.out.println("Start Loading DBPedia Node Map...");

        Model model = ModelFactory.createDefaultModel();
        System.out.println("Loading Node Types...");
        model.read(nodeTypesFilePath);
        StmtIterator typeTriples = model.listStatements();

        while (typeTriples.hasNext()) {

            Statement stmt = typeTriples.nextStatement();
            String subject = stmt.getSubject().getURI().toLowerCase();

            if (subject.length() > 28) {
                subject = subject.substring(28);
            }

            DataNode dataNode;
            String object = stmt.getObject().asResource().getLocalName().toLowerCase();

            int nodeId = subject.toLowerCase().hashCode();

            if (!nodeMap.containsKey(nodeId)) {
                dataNode = new DataNode(subject, NodeType.Entity, "Entity");
                dataNode.addTypes(object);
                dataNode.setType(object);
                nodeMap.put(nodeId, dataNode);


            } else {

                dataNode = nodeMap.get(nodeId);
                dataNode.addTypes(object);


            }

        }

        System.out.println("Done Loading DBPedia Node Map!!!");
        System.out.println("DBPedia NodesMap Size: " + nodeMap.size());


    }

    private void loadGraph(String dataGraphFilePath) {

        if (dataGraphFilePath == null || dataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph Data File Path!");
            return;
        }

        Model model = ModelFactory.createDefaultModel();
        System.out.println("Loading DBPedia Graph...");
        model.read(dataGraphFilePath);
        StmtIterator dataTriples = model.listStatements();

        int loseCount = 0;
        int loopCount = 0;

        while (dataTriples.hasNext()) {

            Statement stmt = dataTriples.nextStatement();
            String subject = stmt.getSubject().getURI();

            if (subject.length() > 28) {
                subject = subject.substring(28).toLowerCase();
            }

            String predicate = stmt.getPredicate().getLocalName().toLowerCase();
            RDFNode object = stmt.getObject();
            String objectString;

            try {

                if (object.isLiteral()) {
                    objectString = object.asLiteral().getString();
                } else {
                    objectString = object.asResource().getLocalName().toLowerCase();
                }

            } catch (DatatypeFormatException e) {
                System.out.println("Invalid DataType Skipped!");
                e.printStackTrace();
                continue;
            }

            int subjectNodeId = subject.hashCode();
            int objectNodeId = objectString.hashCode();

            if (!nodeMap.containsKey(subjectNodeId)
                    || (object.isURIResource() && !nodeMap.containsKey(objectNodeId))) {
                loseCount++;
                continue;
            }

            DataNode currentSubject = nodeMap.get(subjectNodeId);
            DataNode currentObject;

            if (object.isURIResource()) {

                if (subjectNodeId == objectNodeId) {
                    loopCount++;
                    continue;
                }
                currentObject = nodeMap.get(objectNodeId);

            } else if (object.isLiteral()) {

                   currentObject = new DataNode(objectString, NodeType.Literal, "Literal");
                   this.dataGraph.addVertex(currentObject);



            } else {

                loseCount++;
                continue;

            }

            dataGraph.addEdge(currentSubject, currentObject, new RelationshipEdge(predicate));


        }

        System.out.println(loopCount + "Loops!");
        System.out.println(loseCount + "Loses!");
        System.out.println("Number of Edges: " + dataGraph.edgeSet().size());
        System.out.println("Number of Nodes: " + dataGraph.vertexSet().size());
        System.out.println("Done Loading DBPedia Graph!!!");

    }

}
