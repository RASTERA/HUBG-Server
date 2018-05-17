import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 13412431243L;

    float x;
    float y;
    float rotation;
    int[] guns = new int[2];
    String name;

    public Player(float x, float y, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public void setLocation(float[] location) {
        this.x = location[0];
        this.y = location[1];
    }
}