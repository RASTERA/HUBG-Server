// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// Game.java | Central game logic

package com.rastera.Networking;

import org.json.JSONObject;

import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

class Game {

    // Keep track of players
    private final ArrayList<ClientConnection> clientList;
    private final LinkedBlockingQueue<Message> gameMessage;
    private final ArrayList<String> deadQueue;
    private final HashMap<String, Player> playerList ;

    public Game() {
        clientList = new ArrayList<>();
        gameMessage = new LinkedBlockingQueue<>();
        playerList= new HashMap<>();
        deadQueue = new ArrayList<>();

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

                        System.out.println("Receive location update");

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
                                    if (victimPlayer != null && victimPlayer.hit(1)) {
                                        killPlayer(victimPlayer.name, attackerPlayer.name, info.getString("weapon"));
                                        System.out.println("player " + victimPlayer.name + " is dead");
                                    }

                                    broadcast(MessageBuilder.messageBuilder(11, mainMessage.message));

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

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
                    System.out.println("Communicated " + name);
                    conn.write(MessageBuilder.messageBuilder(14, player.health));
                    conn.write(MessageBuilder.messageBuilder(16, player.energy));
                    break;
                }
            }
        }
    }

    // Get player object from UID
    private Player getPlayerFromID(int id) {
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
                currentPlayer = new Player((int) (10000 * Math.random()), (int) (10000 * Math.random()), (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
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

        // Announce if they were killed last round
        if (deadQueue.contains(conn.name)) {
            deadQueue.remove(conn.name);

            conn.write(MessageBuilder.messageBuilder(-3, "You were killed in the last round."));
        }
    }
}