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
        in = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());

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
            out.writeObject(obj);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
}
