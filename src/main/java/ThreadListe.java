import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

public class ThreadListe extends Thread{

    private static final String QUEUE_NAME = "client_to_server";
    private Semaphore sem;
    private ConcurrentHashMap<UUID,Actiune> listaCereri;
    private ConcurrentHashMap<UUID,Actiune> listaOferte;
    private List<String> istoric;
    private ConnectionFactory factory;

    public ThreadListe(ConnectionFactory factory,Semaphore sem, ConcurrentHashMap<UUID,Actiune> listaCereri, ConcurrentHashMap<UUID,Actiune> listaOferte,List<String> istoric) {
        this.sem = sem;
        this.listaCereri=listaCereri;
        this.listaOferte=listaOferte;
        this.istoric=istoric;
        this.factory=factory;
    }

    @Override
    public void run() {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.queuePurge(QUEUE_NAME);

            channel.basicQos(1);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                if (message.equals("getActionList")) {
                    try {
                        //send list
                        sem.acquire();
                        String msg = "Oferte:\n" + listaOferte + "\nCereri: \n" + listaCereri;
                        channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                replyProps,
                                msg.getBytes(StandardCharsets.UTF_8));
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("S-a transmis lista cu oferte");
                    }
                } else if (message.equals("getHistory")) {
                    try {
                        //send history
                        sem.acquire();
                        String msg = istoric.toString();
                        channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                replyProps,
                                msg.getBytes(StandardCharsets.UTF_8));
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("S-a transmis istoricul");
                    }
                }
            };
            channel.basicConsume(QUEUE_NAME, false, deliverCallback, (consumerTag -> {
            }));
            while (true) {
                if (sem.availablePermits() == 0)
                    sem.release();
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

}
