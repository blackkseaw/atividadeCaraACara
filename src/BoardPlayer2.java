import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BoardPlayer2 implements Runnable {

    private static final String HOST = "127.0.0.1";
    private PlayersSocketServer playersSocketServer;
    private Scanner scanner = new Scanner(System.in);
    private String userName;
    private String userChar;
    private boolean isMyTurn = false;
    private boolean hasReceivedQuestion = false;

    private List<String> characters = new ArrayList<>(List.of("Leah", "Abigail", "Sam", "Sebastian", "Robin", "Alex", "Junimo", "Prefeito Luis"));
    private List<String> onBoardCharacters = new ArrayList<>(List.of("Leah", "Abigail", "Sam", "Sebastian", "Robin", "Alex", "Junimo", "Prefeito Luis"));


    public void start() throws IOException {
        playersSocketServer = new PlayersSocketServer(new Socket(HOST, BoardServer.PORT));
        System.out.println("Novo player conectado ao servidor.");
        new Thread(this).start();
        configUser();
        gameLoop();
    }

    public void configUser() {
        System.out.println("Bem vindo ao cara a cara da vila pelicanos!");
        System.out.println("Insira seu nome de usuário: ");
        userName = scanner.nextLine();

        System.out.println("Escolha seu personagem:");
        for (int i = 0; i < characters.size(); i++) {
            System.out.println((i + 1) + ". " + characters.get(i));
        }
        int choice = scanner.nextInt();
        userChar = characters.get(choice - 1);
        scanner.nextLine();

        System.out.println("Olá " + userName + "! Você escolheu o personagem " + userChar);
    }

    public synchronized void gameLoop() {
        while (true) {
            while (!isMyTurn) {
                try {
                    System.out.println("Vez do outro jogador, aguarde!");
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("LEMBRETES: " +
                    "- Você escolheu o personagem " + userChar + "\n" +
                    "- Personagens disponíveis no tabuleiro: ");
            for (int i = 0; i < onBoardCharacters.size(); i++) {
                System.out.println((i + 1) + ". " + onBoardCharacters.get(i));
            }

            if (hasReceivedQuestion) {
                respondToQuestion();
            } else {
                System.out.println("Aguardando pergunta do outro jogador...");
            }

            isMyTurn = false;
            notifyAll();
        }
    }

    public void respondToQuestion() {
        String answer;
        System.out.println("Responda a pergunta do outro jogador: ");
        answer = scanner.nextLine();
        playersSocketServer.sendQuestion(answer);
        hasReceivedQuestion = false;
    }

    @Override
    public void run() {
        String message;
        while ((message = playersSocketServer.receiveQuestion()) != null) {
            if (message.startsWith("Palpite: ")) {
                handleGuess(message);
            } else {
                handleQuestion(message);
            }
            synchronized (this) {
                isMyTurn = true;
                notifyAll();
            }
        }
    }

    private void handleGuess(String message) {
        String guessedCharacter = message.substring(9);
        if (guessedCharacter.equals(userChar)) {
            System.out.println("O outro jogador acertou! Você perdeu.");
            // Finalizar jogo ou realizar ações necessárias em caso de perda
        } else {
            System.out.println("Palpite errado. Você ainda está no jogo.");
        }
    }

    private void handleQuestion(String message) {
        System.out.println("Pergunta do outro jogador: " + message);
        hasReceivedQuestion = true;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserChar() {
        return userChar;
    }

    public static void main(String[] args) {
        try {
            BoardPlayer2 client = new BoardPlayer2();
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Cliente finalizado!");
    }
}
