import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class Client {
    private UUID idClient;
    private Scanner scanner = new Scanner(System.in);

    public void runClient() throws IOException, TimeoutException {
        System.out.println("Optiuni\n1.Cere lista actiuni\n2.Postare cerere\n3.Editeaza o actiune\n4.Afiseaza istoricul tranzactiilor\n5.Posteaza oferta\n6.Iesire");

        //meniu
        while (true) {
            String opt = this.scanner.nextLine();
            switch (opt) {
                case "1":
                    System.out.println("Actiunile sunt:");
                    DirectExchange.publishMessage();
//                    System.out.println(ActionDb.getActionList());
                    break;
                case "2":
                    System.out.println("Pentru a cumpara o actiune, trebuie sa introduci numele ei, cantitatea si pretul\n ");
                    System.out.println("Numele actiunii: ");
                    String numeActiune = this.scanner.nextLine();
                    System.out.println("Cantitate: ");
                    String cantitate = this.scanner.nextLine();
                    System.out.println("Pret: ");
                    String pret = this.scanner.nextLine();
                    System.out.println("Va multumim!\n");
                    break;
                case "3":
                    System.out.println("Editeaza o actiune...\nScrie ID-ul actiunii ");
                    String idNou = this.scanner.nextLine();
                    System.out.print("\nScrie noul nume: ");
                    String numeNou = this.scanner.nextLine();
                    System.out.print("\nScrie noul pret: ");
                    String pretNou = this.scanner.nextLine();
                    System.out.print("\nScrie noua cantitate: ");
                    String cantitateNou = this.scanner.nextLine();
                    break;
                case "4":
                    System.out.println("Se afiseaza istoricul tranzactiilor...\n");
                    break;
                case "5":
                    //postare
                    System.out.print("\nPosteaza oferta\n\nIntrodu numele actiunii:");
                    String numeActiuneP = this.scanner.nextLine();
                    System.out.print("Scrie pretul actiunii: ");
                    float pretP = this.scanner.nextFloat();
                    System.out.print("Scrie cantitatea: ");
                    int cantitateP = this.scanner.nextInt();
                    UUID idActiune = UUID.randomUUID();
                    Actiune actiune = new Actiune(idActiune, idClient, Config.type_oferta, numeActiuneP, cantitateP, pretP);
                    DirectExchange.subscribeMessage();
                    ActionDb.saveAction(actiune);
                    DirectExchange.publishMessage();
                    break;
                case "6":
                    System.out.println("Iesire... Va mai asteptam!");
                    System.exit(0);
                    break;
            }
        }
    }

}
