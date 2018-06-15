// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// Game.java | Central game logic

package com.rastera.Networking;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Game{
    private ArrayList<ClientConnection> clientList;
    private LinkedBlockingQueue<Message> gameMessage;
    private ArrayList<String> deadQueue;
    public ConcurrentHashMap<String, Player> playerList ;
    private ConcurrentHashMap<Long, ArrayList<long[]>> masterItemList = new ConcurrentHashMap<>();
    private int maxItemID = -1000;
    private ConcurrentHashMap<Integer, Integer> damage = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> name = new ConcurrentHashMap<>();

    public Game() {
        clientList = new ArrayList<>();
        gameMessage = new LinkedBlockingQueue<>();
        playerList= new ConcurrentHashMap<>();
        deadQueue = new ArrayList<>();

        try {
            Scanner input = new Scanner(new File("assets/itemData.txt"));
            int id;
            String[] data;

            input.nextLine();

            while (input.hasNext()) {
                data = input.nextLine().split(",");
                id = Integer.parseInt(data[0]);
                name.put(id, data[1]);
                masterItemList.put((long) id, new ArrayList<long[]>());
                System.out.println(id);
                maxItemID = Math.min(maxItemID, id);
            }

            input = new Scanner(new File("assets/weaponData.txt"));

            input.nextLine();

            while (input.hasNext()) {
                data = input.nextLine().split(",");
                id = Integer.parseInt(data[0]);
                damage.put(id, Integer.parseInt(data[5]));
            }

            startGame();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Apply regen to players every 10 seconds
        Thread regenThread = new Thread(() -> {
            while (true) {
                try {
                    regen();
                    Thread.sleep(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        regenThread.start();

        // Game logic and processor thread
        // Location and kills
        Thread GameProcessor = new Thread(() -> {
            while (true) {
                try {
                    while (true) {
                        Message mainMessage = gameMessage.take();


                        switch (mainMessage.type) {
                            case 10: // Location update
                                broadcast(mainMessage);
                                break;
                            case 11: // Shooting
                                JSONObject info = new JSONObject((String) mainMessage.message);

                                try {
                                    Player victimPlayer = getPlayerFromID(info.getInt("enemy"));
                                    Player attackerPlayer = getPlayerFromID(info.getInt("attacker"));

                                    // Null = shoot at air
                                    if (victimPlayer != null && victimPlayer.hit(damage.get(info.getInt("weapon")))) {
                                        killPlayer(victimPlayer.name, attackerPlayer.name, name.get(info.getInt("weapon")));
                                        System.out.println("player " + victimPlayer.name + " is dead");
                                    }

                                    broadcast(MessageBuilder.messageBuilder(11, mainMessage.message));

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                break;
                            case 31:
                                broadcast(mainMessage);
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        GameProcessor.setDaemon(true);
        GameProcessor.start();

    }

    // Regen health
    private void regen() {
        System.out.println("Trying to update health...");

        Player player;

        // Loops through all players alive
        for (String name : playerList.keySet()) {
            player = playerList.get(name);

            // Restricts health and energy
            player.health = Math.min(100, player.health + 1);
            player.energy = Math.min(100, player.energy + 1);

            System.out.println("Updating " + name);

            // Locate connection if connected
            for (ClientConnection conn : clientList) {
                if (conn.name.equals(name)) {
                    System.out.println("Communicated " + name + " H:" + player.health + " E:" + player.energy);
                    conn.write(MessageBuilder.messageBuilder(14, player.health));
                    conn.write(MessageBuilder.messageBuilder(16, player.energy));
                    break;
                }
            }
        }
    }

    public boolean takeItem(long[] item) {
        ArrayList<long[]> itemarray = masterItemList.get((long) item[2]);

        for (int i = 0; i < itemarray.size(); i++) {
            if (itemarray.get(i)[0] == item[0] && itemarray.get(i)[1] == item[1]) {
                itemarray.remove(i);
                broadcast(MessageBuilder.messageBuilder(21, item));
                return true;
            }
        }
        return false;
    }

    // Get player object from UID
    public Player getPlayerFromID(int id) {
        for (String name : playerList.keySet()) {
            if (name.hashCode() == id) {
                return playerList.get(name);
            }
        }

        return null;
    }

    // Send message to all clients
    private void broadcast(Message message) {
        for (ClientConnection client : clientList) {
            client.write(message);
        }
    }

    // Handle death
    private void killPlayer(String targetName, String killer, String weapon) {
        Player player = playerList.get(targetName);

        boolean playerConnected = false;

        // Searches for player if connected
        for (ClientConnection conn : clientList) {
            if (conn.player == player) {
                conn.write(MessageBuilder.messageBuilder(-3, String.format("You were killed by %s with %s.", killer, weapon)));
                playerConnected = true;
                break;
            }
        }

        // Queues death message for next connection
        if (!playerConnected) {
            System.out.println("Added to death queue");
            deadQueue.add(targetName);
        }

        // Reports kill to central auth server
        Communicator.updateKills(killer, targetName, weapon);

        broadcast(MessageBuilder.messageBuilder(15, targetName.hashCode())); // Remove player
        broadcast(MessageBuilder.messageBuilder(13, playerList.size())); // Update player count
        broadcast(MessageBuilder.messageBuilder(12, String.format("%s was killed by %s with %s.", targetName, killer, weapon))); // Broadcast death

        playerList.remove(targetName);
    }

    // Remove player from match (connection)
    public void removePlayer(ClientConnection conn) {
        clientList.remove(conn);
    }

    // Add player to match
    public void addPlayer(ClientConnection conn) {
        clientList.add(conn);

        // Positions of all current players
        ArrayList<long[]> locations = new ArrayList<>();

        System.out.println("ADDED PLAYER " + conn.name);

        Player currentPlayer;
        Random rand = new Random();

        if (playerList.containsKey(conn.name)) {
            currentPlayer = playerList.get(conn.name);
        } else {
            Communicator.updateMatches(conn.name);

            // Randomizes position if not in development
            if (Communicator.developmentMode) {
                currentPlayer = new Player(1000, 1000, (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
            } else {
                //currentPlayer = new Player((int) (10000 * Math.random()), (int) (10000 * Math.random()), (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
                currentPlayer = new Player(1000, 1000, (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
            }

            playerList.put(conn.name, currentPlayer);
        }


        conn.setPlayer(currentPlayer);
        conn.setMessageQueue(gameMessage);

        // Add other player locations to "update bundle"
        for (String username : playerList.keySet()) {
            Player user = playerList.get(username);

            locations.add(new long[] {(long) (user.x * 1000f), (long) (user.y * 1000f), (long) (user.rotation * 1000f), username.hashCode()});
        }

        broadcast(MessageBuilder.messageBuilder(1, locations)); // Announce new user with positions of current players
        broadcast(MessageBuilder.messageBuilder(13, playerList.size())); // Update player count
        //broadcast(MessageBuilder.messageBuilder(17, playerList.keySet().toArray(new String[playerList.size()]))); // Update player names

        // Announce if they were killed last round
        if (deadQueue.contains(conn.name)) {
            deadQueue.remove(conn.name);

            conn.write(MessageBuilder.messageBuilder(-3, "You were killed in the last round."));
        } else {
            conn.write(MessageBuilder.messageBuilder(19, masterItemList));
            conn.write(MessageBuilder.messageBuilder(30, currentPlayer.guns));
            for (String username : playerList.keySet()) {
                conn.write(MessageBuilder.messageBuilder(31, new int []{playerList.get(username).gun, username.hashCode()}));
            }
        }
    }

    public void startGame() {

        Random rand = new Random();

        System.out.println(maxItemID);

        for (int i = 0; i < 10000; i++) {
            masterItemList.get((long) -rand.nextInt(Math.abs(maxItemID+1000)) - 1001).add(new long[] {(long) (rand.nextDouble() * 10000000), (long) (rand.nextDouble() * 10000000)});
        }

        masterItemList.get((long) -rand.nextInt(Math.abs(maxItemID+1000)) - 1001).add(new long[] {1000, 1000});


    }
}