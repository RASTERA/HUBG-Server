import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientConnection {
    ObjectInputStream in;
    ObjectOutputStream out;
    LinkedBlockingQueue<Message> messages;
    Socket socket;

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
                        messages.put(obj);
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
