package Infra;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import java.util.*;

public class MatchResult {

    public int rootIndex;
    public HashMap<DataNode,HashMap<Integer,Integer>> classSet;
    public HashMap<DataNode, String> matchesMap;
    public HashMap<DataNode,Double> costMap;
    public ArrayList<ArrayList<DataNode>> matches;
    public int eid;
    public double maxCost;
    public String root;

    public MatchResult(int eid, String root) {


        this.eid = eid;
        this.classSet = new HashMap<>();
        this.matchesMap = new HashMap<>();
        this.costMap = new HashMap<>();
        this.maxCost = 0.0;
        this.root = root;
    }

    public void partitionResultSets(HashMap<DataNode, String> matches) {


        int current = 1;
        HashSet<DataNode> visited = new HashSet<>();

        for (DataNode nodeA :  classSet.keySet()) {

            if (visited.contains(nodeA)) {
                continue;
            }
            visited.add(nodeA);
            int currentEid;
            if (classSet.get(nodeA).size() == 0) {
                currentEid = current;
                classSet.get(nodeA).put(eid,currentEid);
            } else {
                currentEid = classSet.get(nodeA).get(eid);
            }
            for (DataNode nodeB :  classSet.keySet()) {


                if (nodeA.equals(nodeB)|| visited.contains(nodeB)) {
                    continue;
                }

                if (matches.get(nodeA).equals(matches.get(nodeB))) {
                    classSet.get(nodeB).put(eid,currentEid);
                    visited.add(nodeB);
                }


            }
            current++;

        }

//        System.out.println(classSet.size() + " Classes.");
//        for (DataNode dataNode : classSet.keySet()) {
//            HashMap<Integer, Integer> eids = classSet.get(dataNode);
//            System.out.println("Name : " + dataNode.getNodeName());
//            for (Integer eid : eids.keySet()) {
//                System.out.print("Eid " + eid + " : ");
//                System.out.println(eids.get(eid));
//            }
//            System.out.println("--------");
//        }

    }





    public void classifyResultSets() {

        int current = 1;
        HashSet<Integer> duplicates = new HashSet<>();
        for (int i = 0; i < matches.size(); i++) {

            ArrayList<DataNode> match = matches.get(i);

            if (duplicates.contains(i)) {
                continue;
            }
            duplicates.add(i);
            HashMap<Integer,Integer> currentMap = new HashMap<>();
            currentMap.put(eid,current);
            classSet.put(match.get(rootIndex),currentMap);

            for (int j = 0; j < matches.size(); j++) {

                ArrayList<DataNode> currentMatch = matches.get(j);
                if (i == j || duplicates.contains(j)) {
                    continue;
                }

                if (isEquivalentEntity(match, currentMatch)) {

                    HashMap<Integer,Integer> compareMap = new HashMap<>();
                    compareMap.put(eid, current);
                    classSet.put(currentMatch.get(rootIndex),compareMap);
                    duplicates.add(j);

                }

            }
            current++;

        }

        System.out.println(classSet.size() + " Classes.");

//        for (DataNode dataNode : classSet.keySet()) {
//            HashMap<Integer,Integer> eids = classSet.get(dataNode);
//            System.out.println("Name : " + dataNode.getNodeName());
//            for (Integer eid : eids.keySet()) {
//                System.out.print("Eid " + eid + " : ");
//                System.out.println(eids.get(eid));
//            }
//            System.out.println("--------");
//        }

    }

    public void initialCostForTree(OGraph ontology) {

        // decay factor is 0.9 , in this case , at root , becomes 1/
        for (DataNode node : classSet.keySet()) {

             String type = node.getType();
             int dist = ontology.distMap.get(root).get(type);
             costMap.put(node, 0.1 * dist);
         }

    }


    private boolean isEquivalentEntity(ArrayList<DataNode> match,
                                       ArrayList<DataNode> currentMatch) {

        if (match.size() != currentMatch.size()) {
            return false;
        }

        for (int i = 0; i < match.size(); i++) {
            DataNode node1 = match.get(i);
            DataNode node2 = currentMatch.get(i);
            if (node1.getNodeType() == NodeType.Literal && node2.getNodeType() == NodeType.Literal) {
                if (!node1.getNodeName().equals(node2.getNodeName())) {
                    return false;
                }
            }
        }
        return true;
    }



    public void chaseByChildEnum(MatchResult contributingResult,
                                 QueriesDependencyGraph queryGraph,
                                 DBPediaInduceGraphGenerator induceGraphs,
                                 String ogkQueryRoot, String childRoot) {


        HashMap<DataNode,HashMap<Integer,Integer>> contributingMap = contributingResult.classSet;
        for (DataNode dataNode : this.classSet.keySet()) {
            for (DataNode neighbor : Graphs.neighborListOf(induceGraphs.graphMaps.get(ogkQueryRoot), dataNode)) {
                if (contributingResult.classSet.containsKey(neighbor)
                        && induceGraphs.graphMaps.get(ogkQueryRoot).getEdge(dataNode,neighbor).getLabel().
                        equals(queryGraph.childrenMap.get(ogkQueryRoot).get(childRoot))) {
                    classSet.get(dataNode).putAll(contributingMap.get(neighbor));
                    if (classSet.containsKey(dataNode)) {
                        classSet.get(dataNode).putAll(contributingMap.get(neighbor));
                    }


                }
            }
        }
        System.out.println("Start merge for enum!");
        long start = System.nanoTime();
        mergeEquivalentClasses(this.classSet, contributingResult.eid);
        long current = System.nanoTime();
        System.out.println(current - start + "s");

    }

    public void chaseByChildEnum(MatchResult contributingResult,
                                 QueriesDependencyGraph queryGraph,
                                 YagoInduceGraphGenerator induceGraphs,
                                 String ogkQueryRoot, String childRoot) {


        HashMap<DataNode,HashMap<Integer,Integer>> contributingMap = contributingResult.classSet;
        for (DataNode dataNode : this.classSet.keySet()) {
            for (DataNode neighbor : Graphs.neighborListOf(induceGraphs.graphMaps.get(ogkQueryRoot), dataNode)) {
                if (contributingResult.classSet.containsKey(neighbor)
                        && induceGraphs.graphMaps.get(ogkQueryRoot).getEdge(dataNode,neighbor).getLabel().
                        equals(queryGraph.childrenMap.get(ogkQueryRoot).get(childRoot))) {
                    if (classSet.containsKey(dataNode)) {
                        classSet.get(dataNode).putAll(contributingMap.get(neighbor));
                    }


                }
            }
        }
        System.out.println("Start merge for enum!");
        long start = System.nanoTime();
        mergeEquivalentClasses(this.classSet, contributingResult.eid);
        long current = System.nanoTime();
        System.out.println(current - start + "s");

    }

    public void chaseByChild(MatchResult contributingResult,
                             DefaultDirectedGraph<DataNode, RelationshipEdge> induceGraph) {
        if (classSet.size() == 0 || contributingResult.classSet.size() == 0) {
            return;
        }

        ArrayList<DataNode> needToRemove = new ArrayList<>();

        for (DataNode node : classSet.keySet()) {

            boolean isLinked = false;
            Set<RelationshipEdge> outgoingEdges =  induceGraph.outgoingEdgesOf(node);
            for (RelationshipEdge edge : outgoingEdges) {
                   DataNode dst = induceGraph.getEdgeTarget(edge);
                   if (contributingResult.classSet.containsKey(dst)) {
                       HashMap<Integer,Integer> childEid = contributingResult.classSet.get(dst);
                       this.classSet.get(node).putAll(childEid);
                       if(contributingResult == null) {
                           return;
                       }
                       if (costMap.containsKey(node) && contributingResult.costMap.containsKey(dst)) {
                           costMap.put(node,costMap.get(node) + contributingResult.costMap.get(dst) * 0.9);
                       }
                       isLinked = true;
                       break;
                   }

            }
            if (!isLinked) {
                needToRemove.add(node);
            }

        }

        for (int i = 0; i < needToRemove.size(); i++) {
            classSet.remove(needToRemove.get(i));
        }

        System.out.println("Start merge for OGK!");
        long start = System.nanoTime();
        mergeEquivalentClasses(classSet,contributingResult.eid);
        long current = System.nanoTime();
        System.out.println(current - start);

//        System.out.println("After Classes." + classSet.size());
//
//        for (DataNode dataNode : classSet.keySet()) {
//            HashMap<Integer,Integer> eids = classSet.get(dataNode);
//            System.out.println("Name : " + dataNode.getNodeName());
//            for (Integer eid : eids.keySet()) {
//                System.out.print("Eid " + eid + " : ");
//                System.out.println(eids.get(eid));
//            }
//            System.out.println("--------");
//        }

    }




    public void mergeEquivalentClasses(HashMap<DataNode, HashMap<Integer, Integer>> classSet, int childEid) {


            int current = 1;
            HashSet<DataNode> duplicates = new HashSet<>();
            for (DataNode nodeA : classSet.keySet()) {

                if (duplicates.contains(nodeA)) {
                    continue;
                }
                HashMap<Integer,Integer> map1 = classSet.get(nodeA);
                map1.put(this.eid,current);
                duplicates.add(nodeA);

                for (DataNode nodeB : classSet.keySet()) {

                    if (nodeA.equals(nodeB) || duplicates.contains(nodeB)) {
                        continue;
                    }

                    HashMap<Integer,Integer> map2 = classSet.get(nodeB);
                    if (compareHashMap(map1,map2, childEid))  {
                        map2.put(this.eid,current);
                        duplicates.add(nodeB);
                    }

                }
                current++;
            }

    }

    private static boolean compareHashMap(Map<Integer, Integer> map1, Map<Integer, Integer> map2, int childEid) {
        if (map1.size() == 0 || map2.size() == 0) {
            return false;
        }


       if (map1.get(childEid) == map2.get(childEid)) {
            return true;
       }
        return false;
    }

    private boolean isEquivalent(ArrayList<DataNode> match, ArrayList<DataNode> currentMatch) {
        boolean isEqual = true;
        for (int i = 0; i < match.size(); i++) {
            if (!hasIntersection(match.get(i).getTypes(),currentMatch.get(i).getTypes())) {
                isEqual = false;
            }
        }
        return isEqual;
    }

    private boolean hasIntersection(HashSet<String> similarTypesForPatternNode, HashSet<String> types) {

        if (similarTypesForPatternNode == null || types == null) {
            return false;
        }
        for (String type : types) {
            if (similarTypesForPatternNode.contains(type)) {
                return true;
            }
        }
        return false;
    }



    public void setMaxCostForVF2() {

        double max = 0.0;

        for (int i = 0; i < matches.size(); i++) {

            double tempCost = 0.0;

            for (int j = 0; j < matches.get(i).size(); j++) {
                if(costMap.containsKey(matches.get(i).get(j))) {
                    tempCost += costMap.get(matches.get(j).get(i));
                }
            }

            if (tempCost > max) {
                max = tempCost;
            }

        }

        this.maxCost =  max;

    }


    public void setMaxCost() {

        double max = 0.0;

        for (DataNode dataNode : costMap.keySet()) {

            double tempCost;
            tempCost = costMap.get(dataNode);
            if (tempCost > max) {
                max = tempCost;
            }

        }

        this.maxCost =  max;

    }



}


