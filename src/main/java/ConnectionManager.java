import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ConnectionManager {

    private static Connection connection = null;
    public static Connection getConnection() {
        if (connection == null) {
            try {
                ConnectionFactory connectionFactory = new ConnectionFactory();
                connection = connectionFactory.newConnection();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
