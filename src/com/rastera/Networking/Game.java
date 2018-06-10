package com.rastera.Networking;

import org.json.JSONObject;

import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class Game{
    private ArrayList<ClientConnection> clientList;
    private LinkedBlockingQueue<Message> gameMessage;
    private ArrayList<String> deadQueue;
    public HashMap<String, Player> playerList ;

    private ServerSocket serverSocket;

    private boolean Started = false;

    public Game() {
        clientList = new ArrayList<>();
        gameMessage = new LinkedBlockingQueue<>();
        playerList= new HashMap<>();
        deadQueue = new ArrayList<>();

        Thread GameProcessor = new Thread() {
            public void run() {
                startGame();
                while (true) {
                    try {
                        while (true) {
                            Message mainMessage = gameMessage.take();

                            System.out.println("Receive location update");

                            switch (mainMessage.type) {
                                case 10:
                                    broadcast(mainMessage);
                                    break;
                                case 11:
                                    JSONObject info = new JSONObject((String) mainMessage.message);

                                    try {
                                        Player victimPlayer = getPlayerFromID(info.getInt("enemy"));
                                        Player attackerPlayer = getPlayerFromID(info.getInt("attacker"));

                                        // Null = shoot at air
                                        if (victimPlayer != null && victimPlayer.hit(1)) {
                                            killPlayer(victimPlayer.name, attackerPlayer.name, info.getString("weapon"));
                                            System.out.println("player " + victimPlayer.name + " is dead");
                                        }

                                        //victimConnection.write(rah.messageBuilder(11, mainMessage.message));

                                        broadcast(rah.messageBuilder(11, mainMessage.message));

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
            }
        };

        GameProcessor.setDaemon(true);
        GameProcessor.start();

    }

    public Player getPlayerFromID(int id) {
        for (String name : playerList.keySet()) {
            if (name.hashCode() == id) {
                return playerList.get(name);
            }
        }

        return null;
    }

    public void broadcast(Message message) {
        for (ClientConnection client : clientList) {
            client.write(message);
        }
    }

    public boolean hasStarted() {
        return Started;
    }

    public int size() {
        return clientList.size();
    }

    public LinkedList<Player> findPlayersInRange (Message locationUpdate) {
        LinkedList<Player> enemyPlayers = new LinkedList<>();
        return enemyPlayers;
    }

    public void killPlayer(String targetName, String killer, String weapon) {
        Player player = playerList.get(targetName);

        boolean playerConnected = false;

        for (ClientConnection conn : clientList) {
            if (conn.player == player) {
                conn.write(rah.messageBuilder(-3, String.format("You were killed by %s with %s.", killer, weapon)));
                //conn.terminate();
                playerConnected = true;
                break;
            }
        }

        if (!playerConnected) {
            System.out.println("Added to death queue");
            deadQueue.add(targetName);
        }

        Communicator.updateKills(killer, targetName, weapon);

        broadcast(rah.messageBuilder(15, targetName.hashCode()));
        broadcast(rah.messageBuilder(13, playerList.size()));
        broadcast(rah.messageBuilder(12, String.format("%s was killed by %s with %s.", targetName, killer, weapon)));

        playerList.remove(targetName);
    }

    public void removePlayer(ClientConnection conn) {
        clientList.remove(conn);
    }

    public void addPlayer(ClientConnection conn) {
        clientList.add(conn);

        ArrayList<long[]> locations = new ArrayList<>();

        System.out.println("ADDED PLAYER " + conn.name);

        Player currentPlayer;
        Random rand = new Random();

        if (playerList.containsKey(conn.name)) {
            currentPlayer = playerList.get(conn.name);
        } else {
            //currentPlayer = new Player((int) (10000 * Math.random()), (int) (10000 * Math.random()), (float) Math.toRadians(rand.nextFloat() * 360), conn.name);
            currentPlayer = new Player(1000, 1000, (float) Math.toRadians(rand.nextFloat() * 360), conn.name);

            playerList.put(conn.name, currentPlayer);
        }

        conn.setPlayer(currentPlayer);
        conn.setMessageQueue(gameMessage);

        for (String username : playerList.keySet()) {
            Player user = playerList.get(username);

            locations.add(new long[] {(long) (user.x * 1000f), (long) (user.y * 1000f), (long) (user.rotation * 1000f), username.hashCode()});
        }

        broadcast(rah.messageBuilder(1, locations));

        broadcast(rah.messageBuilder(13, playerList.size()));

        if (deadQueue.contains(conn.name)) {
            deadQueue.remove(conn.name);

            conn.write(rah.messageBuilder(-3, "You were killed in the last round."));
        }

        //killPlayer("karlz", "lol", "lol");


        /*
        if (clientList.size() == 2) {
            Thread GameProcessor = new Thread() {
                public void run() {
                    startGame();
                    while (true) {
                        try {
                            while (clientList.size() != 1) {
                                Message mainMessage = gameMessage.take();

                                System.out.println("Receive location update");

                                switch (mainMessage.type) {
                                    case 10:
                                        broadcast(mainMessage);
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            GameProcessor.setDaemon(true);
            GameProcessor.start();
        }*/
    }

    public void startGame() {
        this.Started = true;

        /*
        Player currentPlayer;
        Random rand = new Random();

        ///////////////////////////
        // Temp var
        ArrayList<float[]> locations = new ArrayList<>();


        ///////////////////////////


        for (ClientConnection conn : clientList) {
            currentPlayer = new Player(rand.nextFloat()*-50, rand.nextFloat()*-50, (float) Math.toRadians(rand.nextFloat()*360));
            conn.setPlayer(currentPlayer);
            conn.setMessageQueue(gameMessage);
            //playerList.add(currentPlayer);

            locations.add(new float[] {currentPlayer.x, currentPlayer.y, currentPlayer.rotation, conn.getId()});
        }

        broadcast(rah.messageBuilder(1, locations)); */
    }
}