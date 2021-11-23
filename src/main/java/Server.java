import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class Server {

    private static final String QUEUE_NAME = "client_to_server";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        //ActionDb.saveAction(new Actiune(UUID.randomUUID(),UUID.randomUUID(),"CERERE","nume",100,20.00F));
        try(Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()){
            channel.queueDeclare(QUEUE_NAME,false,false, false, null);
            channel.queuePurge(QUEUE_NAME);

            channel.basicQos(1);

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
                String message = new String(delivery.getBody(), "UTF-8");
                if(message.equals("getActionList")) {
                    try {
                        //send list
                        String msg = ActionDb.getActionList().toString();
                        channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                replyProps,
                                msg.getBytes("UTF-8"));
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } finally {
                        System.out.println("done");
                        synchronized (monitor) {
                            monitor.notify();
                        }
                    }
                }else if(message.equals("getHistory")){
                    try {
                        //send history
                    } finally {
                        System.out.println(" [x] Done");
                    }
                }
            };
            channel.basicConsume(QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
