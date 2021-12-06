import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

public class Server {
    public static final String RED = "\033[0;31m";
    public static final String RESET = "\u001B[0m";

    private static final String QUEUE_NAME = "client_to_server";
    private static final String TRANZACTII_QUEUE_NAME = "queue_tranzactii";
    private static final ConcurrentHashMap<UUID,Actiune> listaCereri = new ConcurrentHashMap<>();
    //private static final List<Actiune> listaCereri = new ArrayList<>(); //puse de cumparatori
    private static final ConcurrentHashMap<UUID,Actiune> listaOferte = new ConcurrentHashMap<>(); //puse de vanzatori
    private static final List<String> istoric = new ArrayList<>();
    static Semaphore sem = new Semaphore(1);

    public static void main(String[] args) throws InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
       // Actiune a = new Actiune(UUID.randomUUID(), UUID.randomUUID(), "OFERTA", "nume", 100, 20.00F);
        //listaOferte.put(UUID.randomUUID(), a);
//        istoric.add(new Actiune(UUID.randomUUID(), UUID.randomUUID(), "CERERE", "istoric", 100, 20.00F));
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
                                UUID idClient = actiuneNoua.getIdClient();
                                listaOferte.put(idClient,actiuneNoua);
                                //vinzi - oferi -> cauti in cerere

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
                                UUID idClient = actiuneNoua.getIdClient();
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
            @Override
            public void run(){
                //iterare pe lista de oferte
                while(true){
                    listaOferte.forEach((keyOferta,valueOferta)->{
                        listaCereri.forEach((keyCerere,valueCerere)->{
                            if(!keyOferta.equals(keyCerere)&&valueOferta.getNume().equals(valueCerere.getNume())&&valueOferta.getPret()==valueCerere.getPret()){
                                if (valueCerere.getCantitate() > valueOferta.getCantitate()) {
                                    istoric.add(valueCerere.myToString() + RED + " MATCH WITH " + RESET + valueOferta.myToString() + "\n");
                                    valueCerere.setCantitate(valueCerere.getCantitate() - valueOferta.getCantitate());
                                    listaOferte.remove(keyOferta,valueOferta);
                                } else if (valueCerere.getCantitate() < valueOferta.getCantitate()) {
                                    istoric.add(valueCerere.myToString() + RED + " MATCH WITH " + RESET + valueOferta.myToString() + "\n");
                                    valueOferta.setCantitate(valueOferta.getCantitate() - valueCerere.getCantitate());
                                    listaCereri.remove(keyCerere,valueCerere);
                                } else if (valueCerere.getCantitate() == valueOferta.getCantitate()) {
                                    istoric.add(valueCerere.myToString() + RED + " MATCH WITH " + RESET + valueOferta.myToString() + "\n");
                                    listaOferte.remove(keyOferta,valueOferta);
                                    listaCereri.remove(keyCerere,valueCerere);
                                }
                            }
                        });
                    });
                }
            }
        };
        threadListe.start();
        threadVanzari.start();
        threadTranzactii.start();

        threadListe.join();
        threadVanzari.join();

    }

    private static boolean findOfertaForCerere(Actiune cerere) {
        for (int i = 0; i < listaOferte.size(); i++) {
            Actiune oferta = listaOferte.get(i);
            if (cerere.getNume().equals(oferta.getNume()) && cerere.getPret() == oferta.getPret() && !cerere.getIdClient().equals(oferta.getIdClient())) {
                if (cerere.getCantitate() > oferta.getCantitate()) {
                    cerere.setCantitate(cerere.getCantitate() - oferta.getCantitate());
                    istoric.add(cerere.myToString() + RED + " MATCH WITH " + RESET + oferta.myToString() + "\n");
                    listaOferte.remove(i);
                } else if (cerere.getCantitate() < oferta.getCantitate()) {
                    oferta.setCantitate(oferta.getCantitate() - cerere.getCantitate());
                    istoric.add(cerere.myToString() + RED + " MATCH WITH " + RESET + oferta.myToString() + "\n");
                    listaCereri.remove(cerere);
                } else if (cerere.getCantitate() == oferta.getCantitate()) {
                    istoric.add(cerere.myToString() + RED + " MATCH WITH " + RESET + oferta.myToString() + "\n");
                    listaOferte.remove(i);
                    listaCereri.remove(cerere);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean findCerereForOferta(Actiune oferta) {
        for (int i = 0; i < listaCereri.size(); i++) {
            Actiune cerere = listaCereri.get(i);
            if (oferta.getNume().equals(cerere.getNume()) && oferta.getPret() == cerere.getPret() && !oferta.getIdClient().equals(cerere.getIdClient())) {
                if (oferta.getCantitate() > cerere.getCantitate()) {
                    oferta.setCantitate(oferta.getCantitate() - cerere.getCantitate());
                    istoric.add(cerere.myToString() + RED + " MATCH WITH " + RESET + oferta.myToString() + "\n");
                    listaCereri.remove(i);
                } else if (oferta.getCantitate() < cerere.getCantitate()) {
                    cerere.setCantitate(cerere.getCantitate() - oferta.getCantitate());
                    istoric.add(cerere.myToString() + RED + " MATCH WITH " + RESET + oferta.myToString() + "\n");
                    listaOferte.remove(oferta);
                } else if (oferta.getCantitate() == cerere.getCantitate()) {
                    istoric.add(cerere.myToString() + RED + " MATCH WITH " + RESET + oferta.myToString() + "\n");
                    listaCereri.remove(i);
                    listaOferte.remove(oferta);
                }
                return true;
            }
        }
        return false;
    }


}
