package com.rastera.Networking;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerTester {
    public static void main(String[] args) {
        try {
            Socket clienta = new Socket(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 25565);
            Socket clientb = new Socket(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 25565);

            client a = new client(clienta);
            client b = new client(clientb);

            a.setDaemon(true);
            b.setDaemon(true);
            a.start();
            b.start();

            Scanner input = new Scanner(System.in);

            String temp = input.next();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class client extends Thread {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private LinkedBlockingQueue<Message> messages;
    private Socket player;


    public client(Socket client) {
        this.player = client;

        this.messages = new LinkedBlockingQueue<>();
        try {
            out = new ObjectOutputStream(client.getOutputStream());
            in = new ObjectInputStream(client.getInputStream());

            out.writeObject(rah.messageBuilder(0, "asd"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                Message obj = (Message) in.readObject();
                System.out.println(obj.type + " " + obj.message);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
