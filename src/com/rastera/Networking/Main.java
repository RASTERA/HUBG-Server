package com.rastera.Networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    public static final String SERVERNAME = "goose";

    public static void main(String[] args) {
        try {
            Server gameServer = new Server(8080);

            Scanner input = new Scanner(System.in);

            String temp = input.next();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Server {
    private Communicator communicator = new Communicator();

    private LinkedBlockingQueue<Message> messages;
    private ServerSocket serverSocket;

    public Server(int port) throws IOException {

        messages = new LinkedBlockingQueue<>();
        serverSocket = new ServerSocket(port);

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

