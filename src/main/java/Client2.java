import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Client2 {
    public static void main(String[] args) throws IOException, TimeoutException {
        Client client1 = new Client();
        client1.runClient();
    }
}
