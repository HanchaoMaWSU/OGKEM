package Infra;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

public class OGK  {

    private HashMap<String, OGKQuery> queries;
    private String keyRoot;
    private HashMap<String, String> literals;

    public OGK (String keysFilePath) {
        queries = new HashMap<>();
        literals = new HashMap<>();
        this.keyRoot = null;
        loadQueryFile(keysFilePath);
    }

    private void loadQueryFile(String keysFilePath) {

        if (keysFilePath == null || keysFilePath.length() == 0) {
            System.out.println("Invalid Keys File!");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(keysFilePath))) {

            HashMap<String, OGKQueryNode> tempNodeMap = new HashMap<>();
            ArrayList<OGKQueryEdge> tempEdgeList = new ArrayList<>();
            String root = null;
            String currentLine;
            while ((currentLine = br.readLine()) != null) {

                if (currentLine.charAt(0) == 'L') {

                    String currentLiteral =  currentLine.substring(2);
                    String[] currentLiteralPair = currentLiteral.split("->");
                    literals.put(currentLiteralPair[0],currentLiteralPair[1]);

                } else if (!currentLine.equals("#")) {

                    String[] triples = currentLine.split(" ");
                    if (triples.length == 1) {
                        System.out.println("length" + triples.length);
                        if (this.keyRoot == null) {
                            this.keyRoot = triples[0];
                        }
                        root = triples[0];
                        continue;
                    }
                    String srcType = triples[0];
                    String dstType = triples[2];
                    String predicate = triples[1];

                    if (!tempNodeMap.containsKey(srcType)) {
                        OGKQueryNode src = new OGKQueryNode(NodeType.Entity, srcType);
                        tempNodeMap.put(srcType, src);
                    }

                    if (!tempNodeMap.containsKey(dstType)) {

                        OGKQueryNode dst;
                        if (dstType.equals("*")) {

                            dstType = predicate;
                            dst = new OGKQueryNode(NodeType.Literal, dstType);
                            tempNodeMap.put(dstType, dst);


                        } else {

                            dst = new OGKQueryNode(NodeType.Entity, dstType);
                            tempNodeMap.put(dstType, dst);

                        }

                    }

                    tempEdgeList.add(new OGKQueryEdge(srcType, dstType, predicate, 0.0));

                } else {

                    for (OGKQueryEdge edge : tempEdgeList) {
                       if (edge.getDst().equals(edge.getEdgePredicate())) {
                           continue;
                       }
                       edge.setWeight(0.0);
                    }

                    HashMap<String, OGKQueryNode> queryNodeMap = new HashMap<>(tempNodeMap);
                    ArrayList<OGKQueryEdge> queryEdgeList = new ArrayList<>(tempEdgeList);

                    tempEdgeList.clear();
                    tempNodeMap.clear();

                    queries.put(root , new OGKQuery(root, queryNodeMap,
                            queryEdgeList,literals,false, false));
                    System.out.println(queries.size());

                }

            }

            br.close();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public String getKeyRoot() {
        return keyRoot;
    }

    public HashMap<String, OGKQuery> getQueries() { return queries; }

}
