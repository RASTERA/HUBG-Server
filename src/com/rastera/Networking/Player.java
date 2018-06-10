package com.rastera.Networking;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 13412431243L;

    float x;
    float y;
    float rotation;
    float health = 100;
    int[] guns = new int[2];
    String name;

    public Player(float x, float y, float rotation, String name) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.name = name;
    }

    public void setLocation(long[] location) {
        this.x = (float) (location[0] / 1000f);
        this.y = (float) (location[1] / 1000f);
    }

    public boolean hit(float dmg) {
        this.health -= dmg;

        if (health <= 0) {
            return true;
        }

        return false;
    }
}