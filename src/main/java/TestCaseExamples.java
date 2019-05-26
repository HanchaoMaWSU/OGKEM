import DBPediaAlgorithmRunner.*;
import DBPediaGraph.DBPediaDataGraph;
import DBPediaOntology.DBPediaOntologyGraph;
import Infra.DBPediaInduceGraphGenerator;
import Infra.DBPediaInduceGraphGenerator2;
import Infra.OGK;
import Infra.QueriesDependencyGraph;

public class TestCaseExamples {

    public static void main(String args[]) {

        DBPediaOntologyGraph oGraph = new DBPediaOntologyGraph(args[0]);
        DBPediaDataGraph graph = new DBPediaDataGraph(args[1], args[2]);
        OGK key = new OGK(args[3]);

        //  Test Cases for OGKEM, GKEM, EnumEm


        System.out.println("OGK Runner :");
        QueriesDependencyGraph dependencyGraph = new QueriesDependencyGraph(key.getQueries(), key.getKeyRoot());
        DBPediaInduceGraphGenerator induceGenerator = new DBPediaInduceGraphGenerator(dependencyGraph, graph, oGraph, 2);
        induceGenerator.generateInduceGraphs();
        OGKEMQueryRunner oRunner = new OGKEMQueryRunner(dependencyGraph, graph, oGraph,
                0.5,2,induceGenerator);
        oRunner.execute();


        System.out.println("GK Runner :");
        QueriesDependencyGraph dependencyGraph1 = new QueriesDependencyGraph(key.getQueries(), key.getKeyRoot());
        DBPediaInduceGraphGenerator2 induceGenerator1 = new DBPediaInduceGraphGenerator2(dependencyGraph1, graph, oGraph, 2);
        induceGenerator1.generateInduceGraphs();
        GKEMQueryRunner gRunner = new GKEMQueryRunner(dependencyGraph1, induceGenerator1);
        gRunner.execute();


        System.out.println("Enum Runner :");
        QueriesDependencyGraph dependencyGraph2 = new QueriesDependencyGraph(key.getQueries(), key.getKeyRoot());
        DBPediaInduceGraphGenerator induceGenerator2 = new DBPediaInduceGraphGenerator(dependencyGraph, graph, oGraph, 2);
        induceGenerator2.generateInduceGraphs();
        EnumEMQueryRunner eRunner = new EnumEMQueryRunner(dependencyGraph2, oGraph, 2,induceGenerator2);
        eRunner.execute();



        // Test cases for BOGKEM, BGKEM and BEnum

        double budget = Double.valueOf(args[4]);

        System.out.println("BOGK Runner :");
        QueriesDependencyGraph dependencyGraph3 = new QueriesDependencyGraph(key.getQueries(), key.getKeyRoot());
        DBPediaInduceGraphGenerator induceGenerator3 = new DBPediaInduceGraphGenerator(dependencyGraph3, graph,
                oGraph, 3);
        induceGenerator3.generateInduceGraphs();
        BOGKEMQueryRunner oRunner1 = new BOGKEMQueryRunner(dependencyGraph3, graph ,oGraph,0.5,3,
                induceGenerator3);

        oRunner1.execute(budget,1);


        System.out.println("BGK Runner :");
        QueriesDependencyGraph dependencyGraph4 = new QueriesDependencyGraph(key.getQueries(), key.getKeyRoot());
        DBPediaInduceGraphGenerator2 induceGenerator4 = new DBPediaInduceGraphGenerator2(dependencyGraph4, graph, oGraph, 3);
        BGKQueryRunner gRunner1 = new BGKQueryRunner(dependencyGraph4, graph ,oGraph,0.5,3,
                induceGenerator4);
        gRunner1.execute(budget,1);


        System.out.println("BEnum Runner :");
        QueriesDependencyGraph dependencyGraph5 = new QueriesDependencyGraph(key.getQueries(), key.getKeyRoot());
        DBPediaInduceGraphGenerator induceGenerator5 = new DBPediaInduceGraphGenerator(dependencyGraph3, graph,
                oGraph, 3);
        BEnumQueryRunner bRunner1 = new BEnumQueryRunner(dependencyGraph5, graph ,oGraph,0.5,3,
                induceGenerator5);
        bRunner1.execute(budget,1);

    }

}





