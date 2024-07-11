import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BoardServer {

    public static final int PORT = 12345;
    private ServerSocket serverSocket;
    private List<PlayersSocketServer> clientes = new ArrayList<PlayersSocketServer>();
    private List<String> characters = Arrays.asList("Leah", "Abigail", "Sam", "Sebastian", "Robin", "Alex", "Junimo", "Prefeito Luis");
    // Salva os personagens escolhidos por cada jogador
    private List<String> chosenCharacters = new ArrayList<>(Arrays.asList(null, null));
    private int currentPlayerIndex = 0; // Jogador atual

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta: " + PORT);
        waitConnections();
    }

    public void waitConnections() throws IOException {
        while (clientes.size() < 2) { // Espera 2 jogadores se conectarem
            PlayersSocketServer socket = new PlayersSocketServer(serverSocket.accept());
            System.out.println("Cliente: " + socket.getRemoteSocketAddress() + " conectado!");
            clientes.add(socket);
            if (clientes.size() == 2) {
                // Inicia o jogo após dois jogadores se conectarem
                System.out.println("Dois jogadores conectados. Iniciando o jogo...");
                for (int i = 0; i < clientes.size(); i++) {
                    int finalI = i;
                    new Thread(() -> waitMessages(clientes.get(finalI), finalI)).start();
                }
            }
        }
    }

    public void waitMessages(PlayersSocketServer socket, int playerIndex) {
        try {
            String question;
            while ((question = socket.receiveQuestion()) != null) {
                System.out.println("Mensagem recebida do cliente " + socket.getRemoteSocketAddress() + " > " + question);
                handleMessage(socket, question, playerIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMessage(PlayersSocketServer socket, String message, int playerIndex) throws IOException {
        if (message.startsWith("Palpite: ")) {
            String guessedCharacter = message.substring(9);
            if (guessedCharacter.equals(chosenCharacters.get(1 - playerIndex))) {
                sendMessageToAll("O jogador " + (playerIndex + 1) + " venceu ao adivinhar o personagem!");
                serverSocket.close();
            } else {
                sendMessageToAll("Palpite errado do jogador " + (playerIndex + 1) + ". O jogo continua.");
            }
        } else {
            sendMessagesToAll(socket, message);
            // Alterna o jogador
            currentPlayerIndex = 1 - currentPlayerIndex;
        }
    }

    public void sendMessagesToAll(PlayersSocketServer sender, String message) {
        for (PlayersSocketServer socket : clientes) {
            if (!sender.equals(socket)) {
                socket.sendQuestion(message);
            }
        }
    }

    public void sendMessageToAll(String message) {
        for (PlayersSocketServer socket : clientes) {
            socket.sendQuestion(message);
        }
    }

    public static void main(String[] args) {
        try {
            BoardServer server = new BoardServer();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}