import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static void main(String[] args) {
        Server gameServer = new Server(25565);
    }
}

class Server {
    private ArrayList<ClientConnection> clientList;
    private LinkedBlockingQueue<Message> messages;
    private ServerSocket serverSocket;

    public Server(int port) {
        clientList = new ArrayList<ClientConnection>();
        messages = new LinkedBlockingQueue<Message>();
        serverSocket = new ServerSocket(port);

        Thread accept = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Socket s = serverSocket.accept();
                        ClientConnection player = new ClientConnection(s, messages);
                        clientList.add(player);
                        player.write(rah.messageBuilder(0,  ""));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        accept.setDaemon(true);
        accept.start();
    }
}

