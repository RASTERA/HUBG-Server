import java.io.Serializable;

public class Player implements Serializable{
    private static final long serialVersionUID = 13412431243L;

    int x;
    int y;
    double rotation;
    int[] guns = new int[2];
    String name;

    public Player(int x, int y, double rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public void setLocation (int[] location) {
        this.x = location[0];
        this.y = location[1];
    }
}