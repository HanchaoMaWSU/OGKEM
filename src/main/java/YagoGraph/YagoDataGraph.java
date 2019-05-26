package YagoGraph;

import Infra.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RiotException;

import java.io.*;
import java.util.HashMap;

public class YagoDataGraph extends DataGraphBase {

    public YagoDataGraph (String nodeTypesFilePath, String dataGraphFilePath,
                          String literalDataGraphFile, String redirectDataFilePath) {
        super();
        loadNodeMap(nodeTypesFilePath);
        loadGraph(dataGraphFilePath, literalDataGraphFile,redirectDataFilePath);
    }

    private void loadNodeMap(String nodeTypesFilePath) {

        if (nodeTypesFilePath == null || nodeTypesFilePath.length() == 0) {
            System.out.println("No Input Node Types File Path!");
            return;
        }

        System.out.println("Start Loading Yago Node Map...");
        Model model = ModelFactory.createDefaultModel() ;
        model.read(nodeTypesFilePath);
        StmtIterator typeTriples = model.listStatements();

        while (typeTriples.hasNext()) {

            Statement stmt  = typeTriples.nextStatement();
            String subject = stmt.getSubject().getURI();

            if (subject.length() > 35) {
                subject = subject.substring(35);
            }

            String object;
            if (stmt.getObject().isURIResource()) {
                object = stmt.getObject().asResource().getURI();
                if (object.length() > 35) {
                    object = object.substring(35);
                }
            } else {
                System.out.println("Invalid  Type Detected!");
                continue;
            }

            int nodeId = subject.hashCode();
            DataNode dataNode;

            if (!nodeMap.containsKey(nodeId)) {

                dataNode = new DataNode(subject, NodeType.Entity, object);
                dataNode.addTypes(object);
                dataNode.setType(object);
                nodeMap.put(nodeId, dataNode);

            } else {
                dataNode = nodeMap.get(nodeId);
                dataNode.addTypes(object);
            }

        }

        System.out.println("Done Loading Yago Node Map!!!");
        System.out.println("Yago Variable Node Map Size:" + nodeMap.size());

    }


    private void loadGraph(String factDataGraphFilePath, String literalFactsGraphFilePath,
                           String redirectFactsGraphFilePath) {

        //In the case of YAGO Data Graph, it includes three facts file , entity relations,
        //date literals and literals, we merge date literals and literals into same index map.

        if (factDataGraphFilePath == null ||
                factDataGraphFilePath.length() == 0) {
            System.out.println("No Input Graph File Path!");
            return;
        }

        if (literalFactsGraphFilePath == null ||
                literalFactsGraphFilePath.length() == 0) {
            System.out.println("No Input Literal Graph File Path!");
            return;
        }

        addAllVertex();
        loadEntityRelations(factDataGraphFilePath);
        loadlFacts(literalFactsGraphFilePath);
        loadlFacts(redirectFactsGraphFilePath);
        System.out.println("Number of Edges: " + dataGraph.edgeSet().size());
        System.out.println("Number of Nodes: " + dataGraph.vertexSet().size());
        System.out.println("Dong Loading Yago Graph!!!");

    }

    private void loadEntityRelations(String factDataGraphFilePath) {

        System.out.println("Start Loading Yago Relation Facts...");
        Model model = ModelFactory.createDefaultModel() ;

        try {
            model.read(factDataGraphFilePath);
        } catch (RiotException e){
            e.printStackTrace();
        }

        StmtIterator relationTriples = model.listStatements();
        int loseCount = 0;
        int loopCount = 0;

        while (relationTriples.hasNext()) {

            Statement stmt  = relationTriples.nextStatement();
            String subject = stmt.getSubject().getURI();

            if (subject.length() > 35) {
                subject = subject.substring(35);
            }

            String predicate = stmt.getPredicate().getLocalName();

            RDFNode object = stmt.getObject();
            if (!object.isURIResource()) {
                System.out.println("Invalid Type Entity (Not URI)");
                continue;
            }

            String objectString = object.asResource().getURI();

            if (objectString.length() > 35) {
                objectString = objectString.substring(35);
            }

            int subjectNodeId = subject.hashCode();
            int objectNodeId = objectString.hashCode();

            if (subjectNodeId == objectNodeId) {
                System.out.println("Loop founed!");
                loopCount++;
                continue;
            }

            if (!nodeMap.containsKey(subjectNodeId) || !nodeMap.containsKey(objectNodeId)) {
                loseCount++;
                continue;
            }


            DataNode currentSubject = nodeMap.get(subjectNodeId);
            DataNode currentObject = nodeMap.get(objectNodeId);

            if (!dataGraph.addEdge(currentSubject, currentObject,
                        new RelationshipEdge(predicate))) {
                    //System.out.println("Failed Adding The Edge!");
            }

        }

        System.out.println(loseCount + "lose in relation facts!" + loopCount + " loops");
        System.out.println("Done Loading Yago Relation Facts!");

    }

    private void loadlFacts(String literalFactsGraphFilePath) {

        System.out.println("Start Loading Yago Literal Facts...");
        Model model = ModelFactory.createDefaultModel() ;

        try {
            model.read(literalFactsGraphFilePath);
        } catch (RiotException e){
            e.printStackTrace();
        }

        StmtIterator literalFactsTriples = model.listStatements();
        int loseCount = 0;
        while (literalFactsTriples.hasNext()) {

            Statement stmt = literalFactsTriples.nextStatement();
            String subject = stmt.getSubject().getURI();
            if (subject.length() > 35) {
                subject = subject.substring(35);
            }
            int subjectNodeId = subject.hashCode();

            if (!nodeMap.containsKey(subjectNodeId)) {
                //System.out.println("Subject not is classified!");
                continue;
            }

            String predicate = stmt.getPredicate().getLocalName();
            RDFNode object = stmt.getObject();

            if (!object.isLiteral()) {
                loseCount++;
                continue;
            }

            String objectString = object.asLiteral().getString();
            int objectNodeId = objectString.hashCode();

            if (!literalNodeMap.containsKey(objectNodeId)) {

                DataNode newLiteralNode = new DataNode(objectString,
                        NodeType.Literal,"Literal");
                literalNodeMap.put(objectNodeId, newLiteralNode);
                dataGraph.addVertex(newLiteralNode);

            }

            DataNode currentSubject = nodeMap.get(subjectNodeId);
            DataNode currentObject = literalNodeMap.get(objectNodeId);

            if (dataGraph.addEdge(currentSubject, currentObject,
                    new RelationshipEdge(predicate))) {

            }

        }
        System.out.println(loseCount + "loses in literal one of literal facts!");

    }

}