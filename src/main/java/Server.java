import com.rabbitmq.client.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class Server {

    private static final ConcurrentHashMap<UUID,Actiune> listaCereri = new ConcurrentHashMap<>();//puse de cumparatori
    private static final ConcurrentHashMap<UUID,Actiune> listaOferte = new ConcurrentHashMap<>(); //puse de vanzatori
    private static final List<String> istoric = new ArrayList<>();
    static Semaphore sem = new Semaphore(1);

    public static void main(String[] args) throws InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        ThreadListe threadListe = new ThreadListe(factory,sem,listaCereri,listaOferte,istoric);
        threadListe.start();

        ThreadVanzari threadVanzari = new ThreadVanzari(factory,sem,listaCereri,listaOferte,istoric);
        threadVanzari.start();

        ThreadTranzactii threadTranzactii = new ThreadTranzactii(factory,listaCereri,listaOferte,istoric);
        threadTranzactii.start();

        threadListe.join();
        threadVanzari.join();
        threadTranzactii.join();
    }

}
