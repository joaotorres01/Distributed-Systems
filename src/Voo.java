import java.io.Serializable;

public class Voo implements Serializable {
    private String origem;
    private String destino;
    private int capacidade;
    private Estado estado;


    public enum Estado{
        POR_DECORRER,
        CANCELADO,
        CONCLUIDO
    }

    public Voo(){
        this.origem = "";
        this.destino = "";
        this.capacidade = 0;
    }

    public Voo(String origem, String destino, int capacidade){
        this.origem = origem;
        this.destino = destino;
        this.capacidade = capacidade;
        this.estado= Estado.POR_DECORRER;
    }

    public Voo(Voo voo){
        this.origem = voo.getOrigem();
        this.destino = voo.getDestino();
        this.capacidade = voo.getCapacidade();
        this.estado = voo.estado;
    }

    public void marcarLugar(){
        this.capacidade-=1;
    }

    public void desmarcarLugar(){
        this.capacidade+=1;
    }

    public boolean semLugares(){
        return this.capacidade ==0;
    }

    public boolean isCancelado (){
        return estado.equals(Estado.CANCELADO);
    }
    public void cancela(){
        this.estado=Estado.CANCELADO;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(int capacidade) {
        this.capacidade = capacidade;
    }


    public Voo clone() {
        return new Voo(this);
    }


    @Override
    public String toString() {
        return "Voo{" +
                "origem='" + origem + '\'' +
                ", destino='" + destino + '\'' +
                ", capacidade=" + capacidade +
                ", estado=" + estado +
                '}';
    }
}
