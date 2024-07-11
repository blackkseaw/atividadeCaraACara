import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

public class PlayersSocketServer {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public PlayersSocketServer(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String receiveQuestion(){
        try {
            return this.in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    public boolean sendQuestion(String question){
        out.println(question);
        return !out.checkError();
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }


}
