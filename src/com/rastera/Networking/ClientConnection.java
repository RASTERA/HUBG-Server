package com.rastera.Networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection {
    private String name;
    public ObjectInputStream in;
    public ObjectOutputStream out;
    public LinkedBlockingQueue<Message> messages;
    private Socket socket;
    public Player player;
    public int id;


    public ClientConnection(Socket socket, LinkedBlockingQueue<Message> messages, int id) throws IOException {
        this.socket = socket;
        this.messages = messages;
        this.id = id;

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        Thread read = new Thread() {
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
            System.out.println("Writing");
            out.writeObject(obj);
            System.out.println("Write Success");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void MessageProcessor(Message obj) {
        try {
            switch (obj.type) {
                case 1:
                    name = ((String[]) obj.message)[0];
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
