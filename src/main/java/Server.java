import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

public class Server {
    public static final String RED = "\033[0;31m";
    public static final String RESET = "\u001B[0m";
    private static final String QUEUE_NAME = "client_to_server";
    private static final String TRANZACTII_QUEUE_NAME = "queue_tranzactii";

    private static final ConcurrentHashMap<UUID,Actiune> listaCereri = new ConcurrentHashMap<>();//puse de cumparatori
    private static final ConcurrentHashMap<UUID,Actiune> listaOferte = new ConcurrentHashMap<>(); //puse de vanzatori
    private static final List<String> istoric = new ArrayList<>();
    static Semaphore sem = new Semaphore(1);

    public static void main(String[] args) throws InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Thread threadListe = new Thread() {
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
        };
        Thread threadVanzari = new Thread() {
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
        };

        Thread threadTranzactii = new Thread(){
            String message="";
            String messageOferta="";
            @Override
            public void run(){
                try (Connection connection = factory.newConnection();
                     Channel channel = connection.createChannel()) {
                    channel.exchangeDeclare("exchangeTranzactii", "direct", true);

                while(true){

                    listaOferte.forEach((keyOferta,valueOferta)->{
                        listaCereri.forEach((keyCerere,valueCerere)->{
                            if(!valueOferta.getIdClient().equals(valueCerere.getIdClient())&&valueOferta.getNume().equals(valueCerere.getNume())&&valueOferta.getPret()==valueCerere.getPret()){
                                if (valueCerere.getCantitate() > valueOferta.getCantitate()) {
                                    message = "S-a efectuat tranzactia: "+ valueCerere.myToString()+" pentru "+valueOferta.getCantitate()+" actiuni";
                                    messageOferta = "S-au vandut actiunile pentru oferta: "+valueOferta.myToString();
                                    try {
                                        if(channel.isOpen()) {
                                            channel.basicPublish("exchangeTranzactii", valueCerere.getIdClient().toString(), null, message.getBytes("UTF-8"));
                                            channel.basicPublish("exchangeTranzactii", valueOferta.getIdClient().toString(), null, messageOferta.getBytes("UTF-8"));
                                        }
                                        } catch (IOException e) {
                                        e.printStackTrace();
                                    }finally {

                                    istoric.add(valueCerere.myToString() + RED + " MATCH WITH " + RESET + valueOferta.myToString() + "\n");
                                    valueCerere.setCantitate(valueCerere.getCantitate() - valueOferta.getCantitate());
                                    listaOferte.remove(keyOferta, valueOferta);
                                }
                                } else if (valueCerere.getCantitate() < valueOferta.getCantitate()) {
                                    message="S-a efectuat tranzactia: "+ valueCerere.myToString();
                                    messageOferta = "S-au vandut "+valueCerere.getCantitate()+" actiuni pentru oferta: "+valueOferta.myToString();
                                    try {
                                        if(channel.isOpen()) {
                                            channel.basicPublish("exchangeTranzactii", valueCerere.getIdClient().toString(), null, message.getBytes("UTF-8"));
                                            channel.basicPublish("exchangeTranzactii", valueOferta.getIdClient().toString(), null, messageOferta.getBytes("UTF-8"));
                                        } } catch (IOException e) {
                                        e.printStackTrace();
                                    }finally {
                                        istoric.add(valueCerere.myToString() + RED + " MATCH WITH " + RESET + valueOferta.myToString() + "\n");
                                        valueOferta.setCantitate(valueOferta.getCantitate() - valueCerere.getCantitate());
                                        listaCereri.remove(keyCerere, valueCerere);
                                    }
                                } else if (valueCerere.getCantitate() == valueOferta.getCantitate()) {
                                    message = "S-a efectuat tranzactia: "+ valueCerere.myToString();
                                    messageOferta = "S-au vandut actiunile pentru oferta: "+valueOferta.myToString();
                                    try {
                                        if (channel.isOpen()) {
                                            channel.basicPublish("exchangeTranzactii", valueCerere.getIdClient().toString(), null, message.getBytes("UTF-8"));
                                            channel.basicPublish("exchangeTranzactii", valueOferta.getIdClient().toString(), null, messageOferta.getBytes("UTF-8"));
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        istoric.add(valueCerere.myToString() + RED + " MATCH WITH " + RESET + valueOferta.myToString() + "\n");
                                        listaOferte.remove(keyOferta, valueOferta);
                                        listaCereri.remove(keyCerere, valueCerere);
                                    }
                                }

                            }
                        });
                    });
                }
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }

            }
        };
        threadListe.start();
        threadVanzari.start();
        threadTranzactii.start();

        threadListe.join();
        threadVanzari.join();
        threadTranzactii.join();
    }

}
