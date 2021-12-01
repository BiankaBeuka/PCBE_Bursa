import com.rabbitmq.client.*;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class Server {
    public static final String RED = "\033[0;31m";
    public static final String RESET = "\u001B[0m";

    private static final String QUEUE_NAME = "client_to_server";
    private static final String TRANZACTII_QUEUE_NAME = "queue_tranzactii";
    private static List<Actiune> listaCereri = new ArrayList<>(); //puse de cumparatori
    private static List<Actiune> listaOferte = new ArrayList<>(); //puse de vanzatori
    private static List<String> istoric = new ArrayList<>();

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        listaOferte.add(new Actiune(UUID.randomUUID(), UUID.randomUUID(), "OFERTA", "nume", 100, 20.00F));
//        istoric.add(new Actiune(UUID.randomUUID(), UUID.randomUUID(), "CERERE", "istoric", 100, 20.00F));
        Thread threadListe = new Thread() {
            @Override
            public void run() {
                try (Connection connection = factory.newConnection();
                     Channel channel = connection.createChannel()) {
                    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                    channel.queuePurge(QUEUE_NAME);

                    channel.basicQos(1);

                    Object monitor = new Object();
                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                                .Builder()
                                .correlationId(delivery.getProperties().getCorrelationId())
                                .build();
                        String message = new String(delivery.getBody(), "UTF-8");
                        if (message.equals("getActionList")) {
                            try {
                                //send list
                                String msg = listaOferte.toString() + "\n" + listaCereri.toString();
                                channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                        replyProps,
                                        msg.getBytes("UTF-8"));
                                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                            } finally {
                                System.out.println("S-a transmis lista cu oferte");
                                synchronized (monitor) {
                                    monitor.notify();
                                }
                            }
                        } else if (message.equals("getHistory")) {
                            try {
                                //send history
                                String msg = istoric.toString();
                                channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                        replyProps,
                                        msg.getBytes("UTF-8"));
                                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                            } finally {
                                System.out.println("S-a transmis istoricul");
                                synchronized (monitor) {
                                    monitor.notify();
                                }
                            }
                        }
                    };
                    channel.basicConsume(QUEUE_NAME, false, deliverCallback, (consumerTag -> {
                    }));
                    while (true) {
                        synchronized (monitor) {
                            try {
                                monitor.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread threadTranzactii = new Thread() {
            @Override
            public void run() {
                try (Connection connection = factory.newConnection();
                     Channel channel = connection.createChannel()) {
                    channel.queueDeclare(TRANZACTII_QUEUE_NAME, false, false, false, null);
                    channel.queuePurge(TRANZACTII_QUEUE_NAME);

                    channel.basicQos(1);

                    Object monitor = new Object();
                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                                .Builder()
                                .correlationId(delivery.getProperties().getCorrelationId())
                                .build();
                        String message = new String(delivery.getBody(), "UTF-8");
                        String[] messageArr = message.split(" ", 2);
                        if (messageArr[0].equals("vindeActiuni")) {
                            try {
                                //send list
                                //actiune = messageArr[1]
                                String msg = messageArr[1];
                                String[] act = messageArr[1].split("=|,");
                                Actiune actiuneNoua = Actiune.toAction(act);
                                listaOferte.add(actiuneNoua);
                                //vinzi - oferi -> cauti in cerere
                                findCerereForOferta(actiuneNoua);
                                channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                        replyProps,
                                        msg.getBytes("UTF-8"));
                                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                            } finally {
                                System.out.println("S-a efectuat vanzarea");
                                synchronized (monitor) {
                                    monitor.notify();
                                }
                            }
                        } else if (messageArr[0].equals("cumparaActiuni")) {
                            try {
                                //send history
                                String msg = messageArr[1];
                                String[] act = messageArr[1].split("=|,");
                                Actiune actiuneNoua = Actiune.toAction(act);
                                listaCereri.add(actiuneNoua);
                                findOfertaForCerere(actiuneNoua);
                                channel.basicPublish("", delivery.getProperties().getReplyTo(),
                                        replyProps,
                                        msg.getBytes("UTF-8"));
                                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                            } finally {
                                System.out.println("S-a efectuat cumpararea");
                                synchronized (monitor) {
                                    monitor.notify();
                                }
                            }
                        }
                    };
                    channel.basicConsume(TRANZACTII_QUEUE_NAME, false, deliverCallback, (consumerTag -> {
                    }));
                    while (true) {
                        synchronized (monitor) {
                            try {
                                monitor.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        };
        threadListe.start();
        threadTranzactii.start();
    }

    private static void findOfertaForCerere(Actiune cerere) {
        for (int i = 0; i < listaOferte.size(); i++) {
            Actiune oferta = listaOferte.get(i);
            if (cerere.getNume().equals(oferta.getNume()) && cerere.getPret() == oferta.getPret() && !cerere.getIdClient().equals(oferta.getIdClient())) {
                if (cerere.getCantitate() > oferta.getCantitate()) {
                    cerere.setCantitate(cerere.getCantitate() - oferta.getCantitate());
                    istoric.add(cerere.myToString() + RED+" MATCH WITH "+RESET + oferta.myToString() + "\n");
                    listaOferte.remove(i);
                } else if (cerere.getCantitate() < oferta.getCantitate()) {
                    oferta.setCantitate(oferta.getCantitate() - cerere.getCantitate());
                    istoric.add(cerere.myToString() + RED+" MATCH WITH "+RESET + oferta.myToString() + "\n");
                    listaCereri.remove(cerere);
                } else if (cerere.getCantitate() == oferta.getCantitate()) {
                    istoric.add(cerere.myToString() + RED+" MATCH WITH "+RESET + oferta.myToString() + "\n");
                    listaOferte.remove(i);
                    listaCereri.remove(cerere);
                }
            }
        }
    }

    private static void findCerereForOferta(Actiune oferta) {
        for (int i = 0; i < listaCereri.size(); i++) {
            Actiune cerere = listaCereri.get(i);
            if (oferta.getNume().equals(cerere.getNume()) && oferta.getPret() == cerere.getPret() && !oferta.getIdClient().equals(cerere.getIdClient())) {
                if (oferta.getCantitate() > cerere.getCantitate()) {
                    oferta.setCantitate(oferta.getCantitate() - cerere.getCantitate());
                    istoric.add(cerere.myToString() + RED+" MATCH WITH "+RESET + oferta.myToString() + "\n");
                    listaCereri.remove(i);
                } else if (oferta.getCantitate() < cerere.getCantitate()) {
                    cerere.setCantitate(cerere.getCantitate() - oferta.getCantitate());
                    istoric.add(cerere.myToString() + RED+" MATCH WITH "+RESET + oferta.myToString() + "\n");
                    listaOferte.remove(oferta);
                } else if (oferta.getCantitate() == cerere.getCantitate()) {
                    istoric.add(cerere.myToString() + RED+" MATCH WITH "+RESET + oferta.myToString() + "\n");
                    listaCereri.remove(i);
                    listaOferte.remove(oferta);
                }
            }
        }
    }


}
