package Infra;

import org.apache.jena.tdb.store.Hash;

import java.util.*;


public abstract class OGraph {

    public HashMap<String, ONode> oNodeIndex;
    public HashMap<String, HashMap<String,Integer>> distMap;
    public HashMap<String, HashSet<String>> similarLabelsMap;

    public OGraph(String inputOntologyFilePath) {
        oNodeIndex = new HashMap<>();
        distMap = new HashMap<>();
        similarLabelsMap = new HashMap<>();
        loadOntologyGraph(inputOntologyFilePath);
    }

    protected abstract void loadOntologyGraph(String inputOntologyFilePath);

    public HashSet<String> getSimilarLabelSet(String label, int alpha) {

        if (similarLabelsMap.containsKey(label)) {
            return similarLabelsMap.get(label);
            //once initial alpha , alpha can not change! We can initial a new OGraph object for further use.
        }
        HashMap<String,Integer> dist;

        if (distMap.containsKey(label)) {
            dist = distMap.get(label);
        } else {
            dist = new HashMap<>();
        }

        if (label == null || label.length() == 0) {
            System.out.println("Invalid Type Due to null!");
            return null;
        }

        HashSet<String> similarLabels = new HashSet<>();
        ONode rootONode = oNodeIndex.get(label);
        System.out.print(label + "   ");
        if (rootONode == null) {
            System.out.println("No this Type!!!");
            similarLabels.add(label);
            return similarLabels;
        }

        Queue<ONode> level = new LinkedList<>();
        level.add(rootONode);
        for (int j = 0; j <= alpha; j++) {

            int size = level.size();
            for (int i = 0; i < size; i++) {

                ONode current = level.remove();
                if (!dist.containsKey(current.getLabelString())) {
                    dist.put(current.getLabelString(),j);
                }
                similarLabels.add(current.getLabelString());
                for (ONode child : current.getChildren()) {
                    level.add(child);
                }
            }

        }
        distMap.put(label, new HashMap<>(dist));
        similarLabelsMap.put(label,new HashSet<>(similarLabels));

        return similarLabels;

    }


}
