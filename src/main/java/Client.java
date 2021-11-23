import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class Client {
    private UUID idClient;
    private Scanner scanner = new Scanner(System.in);
    private ActionDb actionDb=new ActionDb();
    private Channel c;
    private Connection conn;
    private static final String QUEUE_NAME = "client_to_server";
    public Client(){

    }

    public void initialize() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        c = connection.createChannel();
        conn=connection;
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
                    postAction();
                    break;
                case "3":
                    showHistory();
                    break;
                case "4":
                    //postare
                    postOffer();
                    break;
                case "5":
                    System.out.println("Iesire... Va mai asteptam!");
                    System.exit(0);
                    break;
            }
        }
    }

    private void postOffer() throws IOException, TimeoutException {
        System.out.print("\nPosteaza oferta\n\nIntrodu numele actiunii:");
        String numeActiuneP = this.scanner.nextLine();
        System.out.print("Scrie pretul actiunii: ");
        float pretP = this.scanner.nextFloat();
        System.out.print("Scrie cantitatea: ");
        int cantitateP = this.scanner.nextInt();
        UUID idActiune = UUID.randomUUID();
        Actiune actiune = new Actiune(idActiune, idClient, Config.type_oferta, numeActiuneP, cantitateP, pretP);
        ActionDb.saveAction(actiune);
    }

    private void showHistory() {
        System.out.println("Se afiseaza istoricul tranzactiilor...\n");
    }

    private void postAction() {
        System.out.println("Pentru a cumpara o actiune, trebuie sa introduci numele ei, cantitatea si pretul\n ");
        System.out.println("Numele actiunii: ");
        String numeActiune = this.scanner.nextLine();
        System.out.println("Cantitate: ");
        String cantitate = this.scanner.nextLine();
        System.out.println("Pret: ");
        String pret = this.scanner.nextLine();
        System.out.println("Va multumim!\n");
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
        System.out.println("result:     "+result);
        return result;
    }
    public void close() throws IOException {
        conn.close();
    }
    private void receiveFromServer() throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            System.out.println(" [x] Received '" + message + "'");
            try {
                //send list
                System.out.println("received");
                } finally {
                    System.out.println(" [x] Done");
                }
        };
        c.basicConsume("to_client_queue",true,deliverCallback,consumerTag->{});
    }
}
