import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class User implements Serializable {
    private String nome;
    private String pass;
    private Map<Integer,Viagem> viagens; // key -> idReserva
    private boolean loggedIn;

    public User(String nome, String pass, boolean loggedIn){
        this.nome = nome;
        this.pass = pass;
        this.viagens = new HashMap<>();
        this.loggedIn = loggedIn;
    }

    public boolean isLoggedIn(){
        return loggedIn;
    }

    public void logIn(){
        loggedIn = true;
    }
    public void logOut(){
        loggedIn = false;
    }

    public void cancelaViagem (int idReserva)throws Exception{
        if (!viagens.containsKey(idReserva))throw new Exception("ERRO - ID de reserva inválido");
        Viagem v = viagens.get(idReserva);
        if (!LocalDate.now().isAfter(v.getDia())) v.cancelar();
        else throw new Exception("ERRO - Impossível cancelar viagem");
    }

    public void addViagem(Viagem viagem){
        this.viagens.put(viagem.getIdReserva(),viagem);
    }

    public List<Viagem> getViagens (){
        return new ArrayList<>(viagens.values());
    }

    public List<Voo> getVoos(){
        List<Voo> allVoos = new ArrayList<Voo>();
        for (Viagem v : this.viagens.values()){
            allVoos.addAll(v.getVoos());
        }
        return allVoos;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    /*public User clone() {
        return new Voo(this);
    }
    */


}
