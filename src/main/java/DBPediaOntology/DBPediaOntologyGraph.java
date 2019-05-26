package DBPediaOntology;

import Infra.OGraph;
import Infra.ONode;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.*;

public class DBPediaOntologyGraph extends OGraph {

    public DBPediaOntologyGraph(String inputOntologyFilePath) {
        super(inputOntologyFilePath);
    }

    @Override
    public void loadOntologyGraph(String inputOntologyFilePath) {

        if (inputOntologyFilePath == null || inputOntologyFilePath.length() == 0) {
            System.out.println("No Input Ontology Graph File!");
            return;
        }

        System.out.println("Start Loading DBPedia Ontology Graph...");
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        model.read(inputOntologyFilePath);
        ExtendedIterator classes = model.listClasses();

        while (classes.hasNext()) {

            OntClass cls = (OntClass) classes.next();
            String currentLabel = cls.getLocalName();
            if (!oNodeIndex.containsKey(currentLabel)) {
                ONode currentONode = new ONode(cls.getLocalName());
                oNodeIndex.put(currentLabel, currentONode);
            }

            for (Iterator i = cls.listSubClasses(); i.hasNext(); ) {

                OntClass c = (OntClass) i.next();
                String currentSubclassLabel = c.getLocalName().toLowerCase();
                ONode currentSubOntology;
                if (oNodeIndex.containsKey(currentSubclassLabel)) {
                    currentSubOntology = oNodeIndex.get(currentSubclassLabel);
                } else {
                    currentSubOntology = new ONode(currentSubclassLabel);
                }

                ONode currentOntology = oNodeIndex.get(currentLabel);

                currentOntology.addChild(currentSubOntology);
                currentSubOntology.addParent(currentOntology);
                oNodeIndex.put(currentSubclassLabel, currentSubOntology);

            }

        }

        System.out.println("Done loading DBpedia Ontology!");
        System.out.println("Number of Classes!" + oNodeIndex.size());

    }

}
