import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection {
    String name;
    ObjectInputStream in;
    ObjectOutputStream out;
    LinkedBlockingQueue<Message> messages;
    Socket socket;
    Player player;

    public ClientConnection(Socket socket, LinkedBlockingQueue<Message> messages) throws IOException {
        this.socket = socket;
        this.messages = messages;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        Thread read = new Thread(){
            public void run(){
                while(true){
                    try{
                        Message obj = (Message) in.readObject();

                        switch (obj.type) {
                            case 1:
                                name = ((String[]) obj.message)[0];
                                break;
                            case 10:
                                player.setLocation((double[]) obj.message);
                                messages.put(obj);
                                break;
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
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

    public void setPlayer (Player player) {
        this.player = player;
        player.name = this.name;
    }

    public void write(Object obj) {
        try{
            System.out.println("Writing");
            out.writeObject(obj);
            System.out.println("Write Success");
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
}
