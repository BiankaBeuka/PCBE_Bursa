import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Client1 {
    public static void main(String[] args) throws IOException, TimeoutException {
        Client client1 = new Client();
        client1.runClient();
        /*Thread client1 = new Thread(){
            @Override
            public void run() {
                try {
                    Client client=new Client();
                    client.runClient();
                    //DirectExchange.subscribeMessage();
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        };
        client1.start();*/
    }
}
