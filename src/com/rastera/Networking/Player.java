// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// Player.java | Essential player data

package com.rastera.Networking;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 13412431243L;

    float x;
    float y;
    final float rotation;
    float health = 100;
    float energy = 100;
    int[] guns = {0, 0};
    final String name;
    int gun = 0;

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
        System.out.println(this.health);

        return health <= 0;

    }
}