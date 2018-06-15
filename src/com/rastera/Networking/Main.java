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
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

class Main {

    // Server name for authentication purposes
    public static String SERVERNAME;
    public static ArrayList<long[]> validPositions = new ArrayList<>();

    public static long[] randomPosition() {
        return validPositions.get((int) (Math.random() * validPositions.size()));
    }

    public static void main(String[] args) {
        try {

            BufferedReader mapReader = new BufferedReader(new FileReader(new File("assets/map_display.csv")));

            String rawLine;
            String[] line;
            int y = -1;

            while ((rawLine = mapReader.readLine()) != null) {
                line = rawLine.split(",");

                if (y == -1) {
                    y = line.length;
                } else {
                    y--;
                }

                for (int x = 0; x < line.length; x++) {

                    if (line[x].equals("204") || line[x].equals("4")) {
                        validPositions.add(new long[]{(long) x * 10000, (long) y * 10000});
                    }
                }
            }

            System.out.println("Loaded map");
            System.out.println(validPositions.size());

            Server gameServer = new Server();

            Scanner input = new Scanner(System.in);

            String temp = input.next();

            System.gc();

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

