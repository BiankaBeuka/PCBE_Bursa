import java.util.Scanner;
import java.util.UUID;

public class Client {
    private UUID idClient;
    private Scanner scanner = new Scanner(System.in);
    public void runClient() {

        //meniu
        while (true) {
            System.out.println("Optiuni\n1.Cere lista actiuni\n2.Cumpara actiuni\n3.Editeaza o actiune\n4.Afiseaza istoricul tranzactiilor\n5.Posteaza actiune spre cumparare\n6.Iesire");
            String opt = this.scanner.nextLine();
            switch (opt) {
                case "1":
                    System.out.println("Actiunile sunt:\n");
                    //e.addNews(new New(topic,new ArrayList<>(), UUID.randomUUID() , title1));
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
                    //e.modifyNewsDomain(UUID.fromString(newsId1), domain);
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
//                    e.modifyNewsTitle(UUID.fromString(newsId3), title3);
                    break;
                case "4":
                    System.out.println("Se afiseaza istoricul tranzactiilor...\n");
                    //System.out.println("Readers number=" + e.getReadersForNews(UUID.fromString(newsId4)) + "\n");
                    break;
                case "5":
                    //postare
                    System.out.print("\n Posteaza actiune spre cumparare\n Introdu numele actiunii");
                    String numeActiuneP = this.scanner.nextLine();
                    System.out.print("\nScrie pretul actiunii: ");
                    String pretP = this.scanner.nextLine();
                    System.out.print("\nScrie cantitatea: ");
                    String cantitateP = this.scanner.nextLine();

                    break;
                case "6":
                    System.out.println("Iesire... Va mai asteptam!");
                    System.exit(0);
                    break;
                default:
                    System.exit(0);
            }
        }
    }

}
