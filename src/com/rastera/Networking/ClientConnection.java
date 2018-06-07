package com.rastera.Networking;

import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection {
    public String name;
    public ObjectInputStream in;
    public ObjectOutputStream out;
    public LinkedBlockingQueue<Message> messages;
    private Socket socket;
    public Player player;
    public int id;
    public JSONObject user;
    private Thread read;

    private static LinkedBlockingQueue<ClientConnection> waiting = new LinkedBlockingQueue<>();;
    private static int counter = 0;
    private static ArrayList<ClientConnection> clientList = new ArrayList<>();
    private static Game cGame;


    public ClientConnection(Socket socket, LinkedBlockingQueue<Message> messages) throws IOException {
        this.socket = socket;
        this.messages = messages;

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        read = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Message obj = (Message) in.readObject();
                        MessageProcessor(obj);

                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        };

        read.setDaemon(true); // terminate when main ends
        read.start();
    }

    public void setMessageQueue(LinkedBlockingQueue<Message> messages) {
        this.messages = messages;
    }

    public void setPlayer(Player player) {
        this.player = player;
        player.name = this.name;
    }

    public void write(Object obj) {
        try {
            //System.out.println("Writing");
            out.writeObject(obj);
            //System.out.println("Write Success");
        } catch (IOException e) {
            e.printStackTrace();
            terminate();
        }
    }

    public static int acceptPlayer(ClientConnection player) {
        try {
            System.out.println(player.name + " accepted");

            player.write(rah.messageBuilder(0, counter));

            counter++;

            clientList.add(player);
            waiting.add(player);

            if (cGame != null && cGame.size() != 100) {
                cGame.addPlayer(player);
            } else {
                if (!(!true != false)) {
                    cGame = new Game();

                    while (!waiting.isEmpty()) {
                        cGame.addPlayer(waiting.take());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return counter;
    }

    public void terminate() {
        System.out.println("Terminated");

        try {
            this.read.stop();
            this.socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void MessageProcessor(Message obj) {
        try {
            switch (obj.type) {
                case -2:

                    try {

                        String token = (String) obj.message;
                        JSONObject response = Communicator.request(Communicator.RequestType.GET, null, Communicator.getURL(Communicator.RequestDestination.AUTH) + "checkGameAuth/" + token);

                        // Duplicate account = auto reject
                        for (ClientConnection conn : clientList) {
                            if (conn.name.equals(response.getString("username"))) {
                                this.write(rah.messageBuilder(-2, "Error: You are already in game"));
                                terminate();
                                break;
                            }
                        }

                        if(response.getBoolean("message")) {

                            if (!response.getString("server").equals(Main.SERVERNAME)) {
                                this.write(rah.messageBuilder(-2, "Error: Token not valid for this server"));
                                terminate();
                            }

                            JSONObject userData = Communicator.request(Communicator.RequestType.GET, null, Communicator.getURL(Communicator.RequestDestination.API) + "data/" + response.getString("username"));

                            this.name = userData.getString("username");
                            this.user = userData;

                            this.write(rah.messageBuilder(-2, "success"));
                            this.id = acceptPlayer(this);

                        } else {
                            this.write(rah.messageBuilder(-2, "Error: Rejected by HUBG Authentication Server"));
                            terminate();
                        }
                    } catch (Exception e) {
                        this.write(rah.messageBuilder(-2, "Error: Rejected by HUBG Authentication Server"));
                        terminate();

                        e.printStackTrace();

                    }

                    break;

                case -1:
                    this.write(rah.messageBuilder(-1, Main.SERVERNAME));
                    break;

                case 10:
                    player.setLocation((float[]) obj.message);
                    this.messages.put(obj);
                    break;

                case 11:
                    this.messages.put(obj);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return this.id;
    }
}
