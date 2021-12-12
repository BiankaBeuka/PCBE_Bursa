import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

public class ThreadTranzactii extends Thread{
    String message="";
    String messageOferta="";
    private Semaphore sem;
    private ConcurrentHashMap<UUID,Actiune> listaCereri;
    private ConcurrentHashMap<UUID,Actiune> listaOferte;
    private List<String> istoric;
    private ConnectionFactory factory;
    public static final String RED = "\033[0;31m";
    public static final String RESET = "\u001B[0m";

    public ThreadTranzactii(ConnectionFactory factory,Semaphore sem, ConcurrentHashMap<UUID,Actiune> listaCereri, ConcurrentHashMap<UUID,Actiune> listaOferte,List<String> istoric) {
        this.sem = sem;
        this.listaCereri=listaCereri;
        this.listaOferte=listaOferte;
        this.istoric=istoric;
        this.factory=factory;
    }
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
}
