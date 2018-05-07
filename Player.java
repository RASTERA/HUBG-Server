import java.io.Serializable;

public class Player implements Serializable{
    private static final long serialVersionUID = 13412431243L;

    double x;
    double y;
    double rotation;
    int[] guns = new int[2];
    String name;

    public Player(double x, double y, double rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public void setLocation (double[] location) {
        this.x = location[0];
        this.y = location[1];
    }
}