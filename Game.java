import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class Game{
    private ArrayList<ClientConnection> clientList;
    private LinkedBlockingQueue<Message> messages;
    private ArrayList<Player> playerList;
    private ServerSocket serverSocket;
    private boolean Started = false;

    public Game() {
        clientList = new ArrayList<>();
        messages = new LinkedBlockingQueue<>();
        playerList = new ArrayList<>();
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

        if (clientList.size() == 100) {
            Thread GameProcessor = new Thread() {
                public void run() {
                    while (true) {
                        try {
                            startGame();

                            while (clientList.size() != 1) {
                                Message mainMessage = messages.take();

                                switch (mainMessage.type) {
                                    case 10:
                                        broadcast(rah.messageBuilder(20, mainMessage.message));
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            GameProcessor.setDaemon(true);
            GameProcessor.start();
        }
    }

    public void startGame() {
        this.Started = true;

        Player currentPlayer;
        Random rand = new Random();

        for (ClientConnection conn : clientList) {
            currentPlayer = new Player(rand.nextInt(5000), rand.nextInt(5000), Math.toRadians(rand.nextDouble()*360));
            conn.setMessageQueue(messages);
            playerList.add(currentPlayer);
        }

        broadcast(rah.messageBuilder(1, playerList));
    }
}