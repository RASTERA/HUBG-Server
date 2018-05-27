package com.rastera.Networking;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class Game{
    private ArrayList<ClientConnection> clientList;
    private LinkedBlockingQueue<Message> gameMessage;
    private ArrayList<Player> playerList;
    private ServerSocket serverSocket;
    private boolean Started = false;

    public Game() {
        clientList = new ArrayList<>();
        gameMessage = new LinkedBlockingQueue<>();
        playerList = new ArrayList<>();

        Thread GameProcessor = new Thread() {
            public void run() {
                startGame();
                while (true) {
                    try {
                        while (clientList.size() != 1) {
                            Message mainMessage = gameMessage.take();

                            System.out.println("Receive location update");

                            System.out.println(clientList);

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

    public void addPlayer(ClientConnection conn) {
        clientList.add(conn);

        Player currentPlayer;
        Random rand = new Random();

        ArrayList<float[]> locations = new ArrayList<>();

        currentPlayer = new Player(-10,-10, (float) Math.toRadians(rand.nextFloat()*360));
        conn.setPlayer(currentPlayer);
        conn.setMessageQueue(gameMessage);

        locations.add(new float[] {currentPlayer.x, currentPlayer.y, currentPlayer.rotation, conn.getId()});

        broadcast(rah.messageBuilder(1, locations));

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