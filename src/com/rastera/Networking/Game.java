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

    private static Random random = new Random();

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

        Thread messageBroadcasterThread = new Thread(() -> {
            try {
                String msg;
                while (true) {

                    msg = Main.broadcastQueue.take();

                    if (msg != null) {

                        System.out.println("Sending: " + msg);

                        // Searches for player if connected
                        for (ClientConnection conn : clientList) {

                            conn.write(MessageBuilder.messageBuilder(-3, msg));

                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        messageBroadcasterThread.setDaemon(true);
        messageBroadcasterThread.start();

        // Apply regen to players every 10 seconds
        Thread regenThread = new Thread(() -> {
            Long itemCount;

            while (true) {
                try {
                    regen();

                    itemCount = 0L;

                    for (Long id : masterItemList.keySet()) {
                        itemCount += masterItemList.get(id).size();
                    }

                    if (itemCount < 1000) {
                        for (int i = 0; i < 20; i++) {
                            spawnItem();
                        }
                    }

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
                            case 22:
                                long[] data = (long[]) mainMessage.message;

                                masterItemList.get(data[0]).add(new long[]{data[1], data[2]});

                                broadcast(mainMessage);
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
                    conn.write(MessageBuilder.messageBuilder(14, (Float) player.health));
                    conn.write(MessageBuilder.messageBuilder(16, (Float) player.energy));
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

        try {
            player.ammo += player.gunAmmo[0] + player.gunammo[1];

            for (int i = player.ammo; i >= 30; i -= 30) {
                System.out.println("k " + i);
                gameMessage.put(MessageBuilder.messageBuilder(22, new long[]{-1004, (long) (player.x * 1000), (long) (player.y * 1000)}));
            }

            if (player.guns[0] != 0) {
                System.out.println("k " + player.guns[0]);
                gameMessage.put(MessageBuilder.messageBuilder(22, new long[]{player.guns[0], (long) (player.x * 1000), (long) (player.y * 1000)}));
            }
            if (player.guns[1] != 0) {
                System.out.println("k " + player.guns[1]);
                gameMessage.put(MessageBuilder.messageBuilder(22, new long[]{player.guns[1], (long) (player.x * 1000), (long) (player.y * 1000)}));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        ArrayList<String> locations = new ArrayList<>();

        System.out.println("ADDED PLAYER " + conn.name);

        Player currentPlayer;
        Random rand = new Random();
        long[] position;

        if (playerList.containsKey(conn.name)) {
            currentPlayer = playerList.get(conn.name);
        } else {
            Communicator.updateMatches(conn.name);

            // Randomizes position if not in development
            if (!Communicator.developmentMode) {
                currentPlayer = new Player(1000, 1000, (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
            } else {
                position = Main.randomPosition();

                //currentPlayer = new Player((int) (10000 * Math.random()), (int) (10000 * Math.random()), (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
                currentPlayer = new Player(position[0] / 1000f, position[1] / 1000f, (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
            }

            playerList.put(conn.name, currentPlayer);
        }


        conn.setPlayer(currentPlayer);
        conn.setMessageQueue(gameMessage);

        JSONObject userJSON;

        // Add other player locations to "update bundle"
        for (String username : playerList.keySet()) {
            Player user = playerList.get(username);

            userJSON = new JSONObject();

            try {

                userJSON.put("name", username);
                userJSON.put("id", username.hashCode());
                userJSON.put("position", new JSONObject() {
                    {
                        put("x", (long) (user.x * 1000f));
                        put("y",(long) (user.y * 1000f));
                        put("r",(long) (user.rotation * 1000f));
                        put("id", username.hashCode());
                    }
                });

                locations.add(userJSON.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        broadcast(MessageBuilder.messageBuilder(1, locations)); // Announce new user with positions of current players
        broadcast(MessageBuilder.messageBuilder(13, playerList.size())); // Update player count

        // Announce if they were killed last round
        if (deadQueue.contains(conn.name)) {
            deadQueue.remove(conn.name);

            conn.write(MessageBuilder.messageBuilder(-3, "You were killed in the last round."));
        } else {
            conn.write(MessageBuilder.messageBuilder(19, masterItemList));
            System.out.println(Arrays.toString(currentPlayer.guns));
            conn.write(MessageBuilder.messageBuilder(30, currentPlayer.guns));
            for (String username : playerList.keySet()) {
                conn.write(MessageBuilder.messageBuilder(31, new int []{playerList.get(username).gun, username.hashCode()}));
            }
            conn.write(MessageBuilder.messageBuilder(32, new int[] {currentPlayer.ammo, currentPlayer.gunammo[0], currentPlayer.gunammo[1]}));
        }
    }

    public void spawnItem() {
        long[] itemPosition = Main.randomPosition();

        masterItemList.get((long) - Game.random.nextInt(Math.abs(maxItemID+1000)) - 1001).add(itemPosition);
    }

    public void startGame() {

        System.out.println(maxItemID);

        for (int i = 0; i < 1000; i++) {
            spawnItem();
        }

        masterItemList.get((long) -1004).add(new long[] {1010, 1010});
        masterItemList.get((long) -1003).add(new long[] {1005, 1005});


    }
}