package Infra;

public class OGKQueryEdge {



    private String src;
    private String dst;
    private String edgePredicate;
    private double weight;

    public OGKQueryEdge(String src, String dst,
                        String edgePredicate, double weight) {

        this.src = src;
        this.dst = dst;
        this.edgePredicate = edgePredicate;
        this.weight = weight;

    }

    public String getSrc() {
        return src;
    }

    public String getDst() { return dst; }

    public String getEdgePredicate() {
        return edgePredicate;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

}
