package Infra;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.jgrapht.Graph;
import org.jgrapht.alg.isomorphism.VF2AbstractIsomorphismInspector;
import org.jgrapht.alg.isomorphism.VF2SubgraphIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public class DataGraphBase {

    protected Graph<DataNode,RelationshipEdge> dataGraph;
    protected HashMap<Integer,DataNode> nodeMap;
    protected HashMap<Integer,DataNode> literalNodeMap;

    public DataGraphBase( ) {

        nodeMap  = new HashMap<>();
        literalNodeMap = new HashMap<>();
        dataGraph = new DefaultDirectedGraph<>(RelationshipEdge.class);

    }

    protected void addAllVertex() {

        Iterator nodeMapIterator = nodeMap.entrySet().iterator();

        while (nodeMapIterator.hasNext()) {
            Map.Entry currentNodeEntry = (Map.Entry) nodeMapIterator.next();
            dataGraph.addVertex((DataNode) currentNodeEntry.getValue());
        }

        Iterator literalNodeMapIterator = literalNodeMap.entrySet().iterator();

        while (nodeMapIterator.hasNext()) {
            Map.Entry currentNodeEntry = (Map.Entry) nodeMapIterator.next();
            dataGraph.addVertex((DataNode) currentNodeEntry.getValue());
        }

    }

    public HashMap<Integer, DataNode> getNodeMap() {
        return nodeMap;
    }
    public HashMap<Integer, DataNode> getLiteralNodeMap() { return literalNodeMap; }
    public Graph<DataNode, RelationshipEdge> getDataGraph() { return dataGraph; }

}
