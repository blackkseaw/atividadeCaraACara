import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BoardPlayer1 implements Runnable {

    private static final String HOST = "127.0.0.1";
    private PlayersSocketServer playersSocketServer;
    private Scanner scanner = new Scanner(System.in);
    private String userName;
    private String userChar;
    private boolean isMyTurn = true;
    private boolean hasAskedQuestion = false;
    private boolean hasReceivedAnswer = false;
    private boolean player2Turn = false;
    private boolean gameEnded = false;

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
        while (!gameEnded) {
            while (!isMyTurn && !gameEnded) {
                try {
                    System.out.println("Vez do outro jogador, aguarde!");
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            if (gameEnded) break;

            System.out.println("LEMBRETES: " +
                    "- Você escolheu o personagem " + userChar + "\n" +
                    "- Personagens disponíveis no tabuleiro: ");
            for (int i = 0; i < onBoardCharacters.size(); i++) {
                System.out.println((i + 1) + ". " + onBoardCharacters.get(i));
            }

            if (hasReceivedAnswer) {
                playerAction();
            } else if (hasAskedQuestion) {
                System.out.println("Aguardando resposta do outro jogador...");
            } else {
                sendQuestion();
            }

            isMyTurn = false;
            player2Turn = true;
            notifyAll();
        }

        System.out.println("Jogo encerrado!");
    }

    public void sendQuestion() {
        String question;
        System.out.println("Insira a pergunta para o outro jogador: ");
        question = scanner.nextLine();
        playersSocketServer.sendQuestion(question);
        hasAskedQuestion = true;
    }

    public void playerAction() {
        System.out.println("\nSua vez! Escolha uma das ações abaixo: " +
                "\n1. Abaixar personagem" +
                "\n2. Dar palpite" +
                "\n3. Não fazer nada");
        int action = scanner.nextInt();
        scanner.nextLine();

        switch (action) {
            case 1:
                lowerCharacter();
                break;
            case 2:
                guessCharacter();
                break;
            case 3:
                System.out.println("Você optou por não fazer nada nesta rodada.");
                break;
            default:
                System.out.println("Opção inválida. Tente novamente.");
                playerAction();
        }
    }

    public void lowerCharacter() {
        while (true) {
            System.out.println("Personagens disponíveis no tabuleiro:");
            for (int i = 0; i < onBoardCharacters.size(); i++) {
                System.out.println((i + 1) + ". " + onBoardCharacters.get(i));
            }
            System.out.println("\nInsira o número do personagem que deseja abaixar (ou 0 para parar): ");
            int choice = scanner.nextInt();
            scanner.nextLine();
            if (choice == 0) {
                break;
            } else if (choice > 0 && choice <= onBoardCharacters.size()) {
                System.out.println("Você abaixou o personagem: " + onBoardCharacters.remove(choice - 1));
            } else {
                System.out.println("Personagem inválido. Tente novamente.");
            }
        }
    }

    public void guessCharacter() {
        System.out.println("\nPersonagens disponíveis no tabuleiro:");
        for (int i = 0; i < onBoardCharacters.size(); i++) {
            System.out.println((i + 1) + ". " + onBoardCharacters.get(i));
        }
        System.out.println("\nInsira o número do personagem para realizar o palpite: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        String guessedCharacter = onBoardCharacters.get(choice - 1);
        playersSocketServer.sendQuestion("Palpite: " + guessedCharacter);
        System.out.println("Esperando resposta do adversário...");
    }

    public static void main(String[] args) {
        try {
            BoardPlayer1 client = new BoardPlayer1();
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Cliente finalizado!");
    }

    @Override
    public void run() {
        String message;
        while ((message = playersSocketServer.receiveQuestion()) != null && !gameEnded) {
            if (message.startsWith("Palpite: ")) {
                handleGuess(message);
            } else {
                handleAnswer(message);
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
            gameEnded = true;
            playersSocketServer.sendQuestion("O jogo terminou. " + guessedCharacter + " foi acertado!");
        } else {
            System.out.println("Palpite errado. Você ainda está no jogo.");
        }
    }

    private void handleAnswer(String message) {
        System.out.println("Resposta do outro jogador: " + message);
        hasReceivedAnswer = true;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserChar() {
        return userChar;
    }
}
