import com.rabbitmq.client.ConnectionFactory;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class Config {
    public static String EXCHANGE_NAME = "actions_exchange";
    public static String host = "localhost";
    public static String QUEUE_NAME = "read_queue";
    public static ConnectionFactory connectionFactory;
    public static String type_cerere="CERERE";
    public static String type_oferta="OFERTA";

}
