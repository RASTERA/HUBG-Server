import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Game {
    private ArrayList<ClientConnection> clientList;
    private LinkedBlockingQueue<Message> messages;
    private ServerSocket serverSocket;
    private boolean Started = false;

    public Game() {
        clientList = new ArrayList<ClientConnection>();
        messages = new LinkedBlockingQueue<Message>();

        Thread read = new Thread(){
            public void run(){
                while(true){
                    try{
                        Message obj = (Message) in.readObject();
                        messages.put(obj);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
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

    public void addPlayer(ClientConnection player) {
        clientList.add(player);
    }
}