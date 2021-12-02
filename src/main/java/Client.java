import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class Client implements AutoCloseable{
    private UUID idClient;
    private Scanner scanner = new Scanner(System.in);
    private ActionDb actionDb=new ActionDb();
    private Channel c;
    private Connection conn;
    private static final String QUEUE_NAME = "client_to_server";
    private static final String TRANZACTII_QUEUE_NAME = "queue_tranzactii";
    private static final String TRANZACTII2_QUEUE_NAME = "queue_tranzactii2";

    public Client(){

    }

    public void initialize() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        c = connection.createChannel();
        conn=connection;
        idClient = UUID.randomUUID();
    }

    public void runClient() throws IOException, TimeoutException {
        try{
            initialize();
        }catch (IOException | TimeoutException e){
            e.printStackTrace();
        };
        System.out.println("Optiuni\n1.Cere lista actiuni\n2.Cumpara actiuni\n3.Afiseaza istoricul tranzactiilor\n4.Vinde actiuni\n5.Iesire");

        //meniu
        while (true) {
            String opt = this.scanner.nextLine();
            switch (opt) {
                case "1":
                    try{
                        System.out.println(requestActions());
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    };
                    break;
                case "2":
                    try{
                        postCerere();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    };
                    break;
                case "3":
                    try{
                        System.out.println(showHistory());
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    };
                    break;
                case "4":
                    //postare
                    try{
                        postOffer();
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    };
                    break;
                case "5":
                    System.out.println("Iesire... Va mai asteptam!");
                    System.exit(0);
                    break;
            }
        }
    }

    private void postOffer() throws IOException, TimeoutException, InterruptedException {
        System.out.print("\nPosteaza oferta\n\nNumele actiunii:");
        String numeActiuneP = this.scanner.nextLine();
        System.out.print("Cantitatea: ");
        int cantitateP = this.scanner.nextInt();
        System.out.print("Pret: ");
        float pretP = this.scanner.nextFloat();
        UUID idActiune = UUID.randomUUID();
        Actiune actiune = new Actiune(idActiune, idClient, Config.type_oferta, numeActiuneP, cantitateP, pretP);
        final String corrId = UUID.randomUUID().toString();
        //request list
        String replyQueueName = c.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        String message = "vindeActiuni "+ actiune;
        c.basicPublish("", TRANZACTII_QUEUE_NAME, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = c.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        c.basicCancel(ctag);
        System.out.println(result);
    }

    private String showHistory() throws IOException, InterruptedException {
        System.out.println("Se afiseaza istoricul tranzactiilor...\n");
        final String corrId = UUID.randomUUID().toString();
        //request list
        String replyQueueName = c.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        String message = "getHistory";
        c.basicPublish("", QUEUE_NAME, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = c.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        c.basicCancel(ctag);
        return result;
    }

    private void postCerere() throws IOException, InterruptedException {
        System.out.println("Pentru a cumpara o actiune, trebuie sa introduci numele ei, cantitatea si pretul\n ");
        System.out.print("Numele actiunii: ");
        String numeActiune = this.scanner.nextLine();
        System.out.print("Cantitate: ");
        int cantitate = this.scanner.nextInt();
        System.out.print("Pret: ");
        float pret = this.scanner.nextFloat();
        System.out.println("Va multumim!\n");
        Actiune actiune = new Actiune(UUID.randomUUID(), idClient,Config.type_cerere,numeActiune,cantitate,pret);
        final String corrId = UUID.randomUUID().toString();
        //request list
        String replyQueueName = c.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        String message = "cumparaActiuni "+ actiune;
        c.basicPublish("", TRANZACTII2_QUEUE_NAME, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = c.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        c.basicCancel(ctag);
        System.out.println(result);
    }

    private String requestActions() throws IOException, InterruptedException {
        System.out.println("Actiunile sunt:");
        final String corrId = UUID.randomUUID().toString();
        //request list
        String replyQueueName = c.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();
        String message = "getActionList";
        c.basicPublish("", QUEUE_NAME, props, message.getBytes("UTF-8"));

        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String ctag = c.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.offer(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.take();
        c.basicCancel(ctag);
        return result;
    }
    public void close() throws IOException {
        conn.close();
    }
}
