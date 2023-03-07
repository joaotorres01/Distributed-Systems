import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Viagem implements Serializable {
    private int idReserva;
    private List<Voo> voos;
    private LocalDate dia;
    private Estado estado;

    public Viagem(int idReserva, List<Voo> voos, LocalDate dia){
        this.idReserva = idReserva;
        this.voos = voos;
        for (Voo voo : voos)
            voo.marcarLugar();
        this.dia = dia;
        this.estado = Estado.MARCADA;
    }


    public LocalDate getDia() {
        return dia;
    }

    public enum Estado{
        MARCADA{
            @Override
            public String toString() {
                return super.toString();
            }
        },
        CANCELADA{
            @Override
            public String toString() {
                return super.toString();
            }
        },
        CONCLUIDA{
            @Override
            public String toString() {
                return super.toString();
            }
        }
    }


    public Estado getEstado() {
        return estado;
    }

    public String getEstadoString(){
        return estado.toString();
    }

    public void addVoo (Voo voo){
        this.voos.add(voo);
    }


    public void cancelar(){
        this.estado = Estado.CANCELADA;
        for (Voo voo : voos)
            voo.desmarcarLugar();
    }

    public List<Voo> getVoos(){
        return new ArrayList<>(this.voos);
    }


    public int getIdReserva(){
        return this.idReserva;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "Viagem{" +
                "idReserva=" + idReserva +
                ", voos=" + voos +
                ", dia=" + dia +
                ", estado=" + estado +
                '}';
    }
}
