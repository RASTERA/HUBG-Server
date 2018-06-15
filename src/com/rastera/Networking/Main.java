// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// Main.java

package com.rastera.Networking;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

class Main {

    // Server name for authentication purposes
    public static String SERVERNAME;

    public static void main(String[] args) {
        try {
            Server gameServer = new Server();

            Scanner input = new Scanner(System.in);

            String temp = input.next();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// Core server
class Server {

    private Communicator communicator = new Communicator();

    private final LinkedBlockingQueue<Message> messages;
    private ServerSocket serverSocket;

    private JSONObject data;

    public Server() {

        try {
            StringBuilder dataString = new StringBuilder();
            String line;

            // Loads config and sets up socket
            BufferedReader reader = new BufferedReader(new FileReader(new File("config.json")));

            while ((line = reader.readLine()) != null) {
                dataString.append(line);
            }

            data = new JSONObject(dataString.toString());

            System.out.println(data);

            Main.SERVERNAME = data.getString("servername");

            serverSocket = new ServerSocket(data.getInt("port"), 1000000, InetAddress.getByName(data.getString("address")));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        // Handle connections
        messages = new LinkedBlockingQueue<>();

        Thread accept = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Socket s = serverSocket.accept();

                        System.out.println("connection from" + s.getChannel());
                        ClientConnection player = new ClientConnection(s, messages);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        accept.setDaemon(true);
        accept.start();
    }


}

