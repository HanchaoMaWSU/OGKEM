package YagoOntology;

import Infra.OGraph;
import Infra.ONode;
import org.apache.jena.rdf.model.*;

public class YagoOntologyGraph extends OGraph {

    public YagoOntologyGraph(String inputOntologyFilePath) {
        super(inputOntologyFilePath);
    }

    @Override
    public void loadOntologyGraph(String inputOntologyFilePath) {

        Model model = ModelFactory.createDefaultModel() ;
        System.out.println("Loading Yago Ontology Graph...");
        model.read(inputOntologyFilePath);
        StmtIterator ontologyTriples = model.listStatements();

        while (ontologyTriples.hasNext()) {

            Statement stmt  = ontologyTriples.nextStatement();
            String  subject = stmt.getSubject().getURI();
            String  object =  stmt.getObject().asResource().getURI();

            if (subject.length() > 35) {
                subject = subject.substring(35);
            }

            if (object.length() > 35) {
                object = object.substring(35);
            }

            ONode currentOntology;

            if (oNodeIndex.containsKey(object)) {
                currentOntology = oNodeIndex.get(object);
            } else {
                currentOntology = new ONode(object);
            }

            ONode currentSubOntology;
            if (oNodeIndex.containsKey(subject)) {
                currentSubOntology = oNodeIndex.get(subject);
            } else {
                currentSubOntology = new ONode(subject);
            }

            currentOntology.addChild(currentSubOntology);
            if(!oNodeIndex.containsKey(object)) {
                oNodeIndex.put(object,currentOntology);
            }

            currentSubOntology.addParent(currentOntology);

            if (!oNodeIndex.containsKey(subject)){
                oNodeIndex.put(subject,currentSubOntology);
            }

        }

        System.out.println("Done loading Yago ontology!");
        System.out.println("Yago Number of Classes!" + oNodeIndex.size());

    }

}
