import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

public class ThreadVanzari extends Thread{

    private static final String TRANZACTII_QUEUE_NAME = "queue_tranzactii";
    private Semaphore sem;
    private ConcurrentHashMap<UUID,Actiune> listaCereri;
    private ConcurrentHashMap<UUID,Actiune> listaOferte;
    private List<String> istoric;
    private ConnectionFactory factory;

    public ThreadVanzari(ConnectionFactory factory,Semaphore sem, ConcurrentHashMap<UUID,Actiune> listaCereri, ConcurrentHashMap<UUID,Actiune> listaOferte,List<String> istoric) {
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
            channel.queueDeclare(TRANZACTII_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(TRANZACTII_QUEUE_NAME);
            channel.basicQos(1);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String[] messageArr = message.split(" ", 2);
                if (messageArr[0].equals("vindeActiuni")) {
                    try {
                        //send list
                        //actiune = messageArr[1]
                        sem.acquire();
                        String msg = messageArr[1];
                        String[] act = messageArr[1].split("=|,");
                        Actiune actiuneNoua = Actiune.toAction(act);
                        UUID idClient = actiuneNoua.getIdActiune();
                        listaOferte.put(idClient,actiuneNoua);
                        channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                replyProps,
                                msg.getBytes(StandardCharsets.UTF_8));

                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("S-a efectuat vanzarea");

                    }
                } else if (messageArr[0].equals("cumparaActiuni")) {
                    try {
                        //send history
                        sem.acquire(1);
                        String msg = messageArr[1];
                        String[] act = messageArr[1].split("=|,");
                        Actiune actiuneNoua = Actiune.toAction(act);
                        UUID idClient = actiuneNoua.getIdActiune();
                        listaCereri.put(idClient,actiuneNoua);
                        channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                replyProps,
                                msg.getBytes(StandardCharsets.UTF_8));
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println("S-a efectuat cumpararea");
                    }
                }

            };
            channel.basicConsume(TRANZACTII_QUEUE_NAME, false, deliverCallback, (consumerTag -> {
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
