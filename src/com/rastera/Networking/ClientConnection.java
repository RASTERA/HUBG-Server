// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// ClientConnection.java

package com.rastera.Networking;

import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

// Handles socket with client
class ClientConnection {

    // Core socket
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private LinkedBlockingQueue<Message> messages;
    private final Socket socket;
    private Thread read;

    private static final LinkedBlockingQueue<ClientConnection> waiting = new LinkedBlockingQueue<>();
    private static final ArrayList<ClientConnection> clientList = new ArrayList<>();
    private static Game cGame;

    // General information
    public String name;
    public int id;
    public Player player;
    private JSONObject user;

    public ClientConnection(Socket socket, LinkedBlockingQueue<Message> messages) throws IOException {
        this.socket = socket;
        this.messages = messages;

        // Object streams
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // Reader thread
        read = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Message obj = (Message) in.readObject();

                        if (obj.type == 1000) {
                            terminate();
                            break;
                        }

                        MessageProcessor(obj);

                    } catch (Exception e) {
                        e.printStackTrace();
                        terminate();
                        break;
                    }
                }
            }
        };

        read.setDaemon(true); // terminate when main ends
        read.start();
    }

    // Set central message queue
    public void setMessageQueue(LinkedBlockingQueue<Message> messages) {
        this.messages = messages;
    }

    // Sets player object
    public void setPlayer(Player player) {
        this.player = player;
    }

    // Writes to client
    public void write(Object obj) {
        try {
            out.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
            terminate();
        }
    }

    // Accept player to game
    private static void acceptPlayer(ClientConnection player) {
        try {

            System.out.println(player.name + " accepted");

            // Generate ID based on hashcode
            player.id = player.name.hashCode();
            player.write(MessageBuilder.messageBuilder(0, player.id));

            // Adds player to queue if waiting
            clientList.add(player);
            waiting.add(player);

            if (cGame != null) {
                // Forwards player to main game
                cGame.addPlayer(player);
            } else {

                cGame = new Game();

                while (!waiting.isEmpty()) {
                    cGame.addPlayer(waiting.take());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    // Terminate socket
    private void terminate() {
        System.out.println("Terminated " + this.name);

        // Removes remains of player
        if (clientList.contains(this)) {
            clientList.remove(this);
            waiting.remove(this);
            cGame.removePlayer(this);
        }

        try {
            this.read.stop();
            this.socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Process incoming messages
    private void MessageProcessor(Message obj) {
        try {
            switch (obj.type) {
                case -2: // Authentication request

                    try {

                        // Validates token with central authentication server
                        String token = (String) obj.message;
                        JSONObject response = Communicator.request(Communicator.RequestType.GET, null, Communicator.getURL(Communicator.RequestDestination.AUTH) + "checkGameAuth/" + token);

                        // Duplicate account = auto reject
                        for (ClientConnection conn : clientList) {
                            if (conn.name.equals(response.getString("username"))) {
                                this.write(MessageBuilder.messageBuilder(-2, "Error: You are already in game"));

                                terminate();
                                break;
                            }
                        }

                        // If response exists
                        if(response.getBoolean("message")) {

                            // If token is for different server
                            if (!response.getString("server").equals(Main.SERVERNAME)) {
                                this.write(MessageBuilder.messageBuilder(-2, "Error: Token not valid for this server"));
                                terminate();
                            }

                            // If successful, obtain server data
                            JSONObject userData = Communicator.request(Communicator.RequestType.GET, null, Communicator.getURL(Communicator.RequestDestination.API) + "data/" + response.getString("username"));

                            // Update local data
                            this.name = userData.getString("username");
                            this.user = userData;

                            // Returns success
                            this.write(MessageBuilder.messageBuilder(-2, "success"));
                            acceptPlayer(this);

                        } else {
                            this.write(MessageBuilder.messageBuilder(-2, "Error: Rejected by HUBG Authentication Server"));
                            terminate();
                        }
                    } catch (Exception e) {
                        this.write(MessageBuilder.messageBuilder(-2, "Error: Rejected by HUBG Authentication Server"));
                        terminate();

                        e.printStackTrace();

                    }

                    break;

                case -1: // Request servername
                    this.write(MessageBuilder.messageBuilder(-1, Main.SERVERNAME));
                    break;

                case 10: // Update player location
                    this.player.setLocation((long[]) obj.message);
                    this.messages.put(obj);
                    break;

                case 11: // Shooting
                    this.messages.put(obj);
                    break;

                case 14: // Get health
                    this.write(MessageBuilder.messageBuilder(14, this.player.health));

                    break;

                case 16: // Set energy

                    if (obj.message != null) {
                        this.player.energy = (float) obj.message;
                    }

                    this.write(MessageBuilder.messageBuilder(16, this.player.energy));
                    break;

                case 20:
                    this.write(MessageBuilder.messageBuilder(20, cGame.takeItem((long[]) obj.message)));
                    break;
                case 30:
                    player.guns = (int[]) obj.message;
                case 31:
                    player.gun = ((int[]) obj.message)[1];
                    this.messages.put(obj);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
