import java.util.Objects;
import java.util.UUID;

public class Actiune {
    private String type;



    private UUID idClient;
    private UUID idActiune;
    private String nume;
    private int cantitate;
    private float pret;

    public Actiune( UUID idActiune, UUID idClient, String type, String nume, int cantitate, float pret) {
        this.type = type;
        this.idActiune = idActiune;
        this.idClient = idClient;
        this.nume = nume;
        this.cantitate = cantitate;
        this.pret = pret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Actiune actiune = (Actiune) o;
        return cantitate == actiune.cantitate && Float.compare(actiune.pret, pret) == 0 && type.equals(actiune.type) && idActiune.equals(actiune.idActiune) && nume.equals(actiune.nume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, idActiune, nume, cantitate, pret);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getIdActiune() {
        return idActiune;
    }

    public void setIdActiune(UUID idActiune) {
        this.idActiune = idActiune;
    }

    public String getNume() {
        return nume;
    }

    public void setNume(String nume) {
        this.nume = nume;
    }

    public int getCantitate() {
        return cantitate;
    }

    public void setCantitate(int cantitate) {
        this.cantitate = cantitate;
    }

    public float getPret() {
        return pret;
    }

    public void setPret(float pret) {
        this.pret = pret;
    }

    public UUID getIdClient() {
        return idClient;
    }

    @Override
    public String toString() {
        return  "type='" + type + '\'' +
                ", idClient=" + idClient +
                ", idActiune=" + idActiune +
                ", nume='" + nume + '\'' +
                ", cantitate=" + cantitate +
                ", pret=" + pret+"\n" ;
    }

    public String myToString() {
        return  "type='" + type + '\'' +
                ", nume='" + nume + '\'' +
                ", cantitate=" + cantitate +
                ", pret=" + pret ;
    }


    public static Actiune toAction(String[] array){
        String type = array[1].replace("\'","");
        UUID idClient=UUID.fromString(array[3]);
        UUID idActiune=UUID.fromString(array[5]);
        String nume = array[7].replace("\'","");
        int cantitate = Integer.parseInt(array[9]);
        float pret = Float.parseFloat(array[11]);
        return new Actiune(idActiune, idClient, type, nume, cantitate, pret);
    }

}
