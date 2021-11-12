import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DirectExchange {

    //Step-1: Declare the exchange
    public static void declareExchange() throws IOException, TimeoutException {
        Channel channel = ConnectionManager.getConnection().createChannel();
        //Declare my-direct-exchange DIRECT exchange
        channel.exchangeDeclare("my-direct-exchange", BuiltinExchangeType.DIRECT, true);
        channel.close();
    }

    //Step-2: Declare the Queues
    public static void declareQueues() throws IOException, TimeoutException {
        //Create a channel - do not share the Channel instance
        Channel channel = ConnectionManager.getConnection().createChannel();

        //Create the Queues
        channel.queueDeclare("ActionList", true, false, false, null);

        channel.close();
    }

    //Step-3: Create the Bindings
    public static void declareBindings() throws IOException, TimeoutException {
        Channel channel = ConnectionManager.getConnection().createChannel();
        //Create bindings - (queue, exchange, routingKey)
        channel.queueBind("ActionList", "my-direct-exchange", "actions");
        channel.close();
    }

    //Step-4: Create the Subscribers
    public static void subscribeMessage() throws IOException {
        Channel channel = ConnectionManager.getConnection().createChannel();
        channel.basicConsume("ActionList", true, ((idClient, message) -> {
//            System.out.println(idClient);
            System.out.println("ActionList:" + new String(message.getBody()));
        }), idClient -> {
            System.out.println(idClient);
        });

    }

    //Step-5: Publish the messages
    public static void publishMessage() throws IOException, TimeoutException {
        Channel channel = ConnectionManager.getConnection().createChannel();
        String message = ActionDb.getActionList().toString();
        channel.basicPublish("my-direct-exchange", "actions", null, message.getBytes());
        channel.close();
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        DirectExchange.declareQueues();
        DirectExchange.declareExchange();
        DirectExchange.declareBindings();

        //Threads created to publish-subscribe asynchronously
        Thread subscribe = new Thread(){
            @Override
            public void run() {
                try {
//                    Client client=new Client();
//                    client.runClient();
                    DirectExchange.subscribeMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread publish = new Thread(){
            @Override
            public void run() {
                try {
                    Client client=new Client();
                    client.runClient();
                    DirectExchange.publishMessage();
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        };

        subscribe.start();
        publish.start();

    }
}
