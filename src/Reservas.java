import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class Reservas implements Serializable{
    private Map<String,Local> locais;
    private List<Voo> voosDiarios;
    private Map<LocalDate,List<Voo>> todosVoos;
    private Map<LocalDate,List<Viagem>> todasViagens;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public Reservas(){
        this.locais = new HashMap<>();
        this.voosDiarios = new ArrayList<Voo>();
        this.todosVoos = new HashMap<>();
        this.todasViagens = new HashMap<>();
    }

    public List<Voo> getVoosDiarios(){
        return new ArrayList<>(this.voosDiarios);
    }


    public void adicionarVoo(String origem,String destino, int capacidade) throws Exception {
        Voo voo = new Voo(origem, destino, capacidade);
        adicionarVoo(voo);
    }

    public void adicionarVoo (Voo voo) throws Exception{
        try {
            lock.writeLock().lock();
            String origem = voo.getOrigem();
            String destino = voo.getDestino();

            locais.putIfAbsent(origem, new Local(origem));
            locais.putIfAbsent(destino, new Local(destino));

            Local localOrigem = locais.get(origem);
            Local localDestino = locais.get(destino);

            if (localOrigem.containsDestino(localDestino))throw new Exception("ERRO - Voo já existe");
            localOrigem.adicionarDestino(localDestino);

            voosDiarios.add(voo);

            for (Map.Entry<LocalDate,List<Voo>> entry : todosVoos.entrySet()){
                if (!entry.getKey().isBefore(LocalDate.now())) {
                    entry.getValue().add(voo.clone());
                }
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public List<List<String>> getAllCaminhos(String origem, String destino){
        Local localOrigem = locais.get(origem);
        Local localDestino = locais.get(destino);
        return localOrigem.allPercursos(localDestino);
    }

    public boolean validarDestinos(List<String> destinos){
        try {
            lock.readLock().lock();
            for (String destino : destinos) {
                if (!locais.containsKey(destino)) return false;
            }
            return true;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public Viagem marcarViagem(List<String>destinos,LocalDate inicio, LocalDate fim) throws Exception {
        try {
            lock.writeLock().lock();
            for (LocalDate date = inicio; date.isBefore(fim) || date.isEqual(fim); date = date.plusDays(1)) {
                List<Voo> listVoo = new ArrayList<>(); //List dos possiveis voos | reset para cada dia
                String origem = destinos.get(0);
                int lenDestinos = destinos.size();
                for (int i = 1; i < lenDestinos; i++) {
                    todosVoos.putIfAbsent(date,cloneVoosDiarios());//put info num dia que ainda não tinha (? maybe remove if it's not used)
                    List<Voo> voosDia = todosVoos.get(date);
                    String destino = destinos.get(i);
                    for (Voo voo : voosDia) {
                        if (voo.isCancelado()) break; //significa que esse dia foi cancelado
                        if (voo.semLugares()) continue; // capacidade do voo é 0
                        if (voo.getOrigem().equals(origem) && voo.getDestino().equals(destino)) {
                            listVoo.add(voo);
                            origem = destino;
                            break;
                        }
                    }
                }
                if (listVoo.size() == destinos.size() - 1) {//para x destinos é preciso x-1 voos
                    int numReserva = todasViagens.size();
                    Viagem viagem = new Viagem(numReserva, listVoo, date);
                    todasViagens.putIfAbsent(date, new ArrayList<>());
                    List<Viagem> list = todasViagens.get(date);
                    list.add(viagem);
                    //Criar Viagem
                    return viagem;
                }
            }
            throw new Exception("ERRO - Impossível marcar Viagem");
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    private List<Voo> cloneVoosDiarios(){
        List<Voo> res = new ArrayList<>(voosDiarios.size());
        for (Voo voo : voosDiarios)
            res.add(voo.clone());
        return res;
    }


    public void cancelarDia(LocalDate dia){
        try {
            lock.writeLock().lock();
            if (todasViagens.containsKey(dia)) {
                List<Viagem> listaViagens = todasViagens.get(dia);
                for (Viagem viagem : listaViagens) {
                    viagem.cancelar();
                }
            }
            todosVoos.putIfAbsent(dia,cloneVoosDiarios());
            List<Voo> listaVoo = todosVoos.get(dia);
            for (Voo voo : listaVoo) {
                voo.cancela();
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void serialize(String filepath) throws IOException {
        FileOutputStream fos = new FileOutputStream(filepath);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(this);
        oos.close();
        fos.close();
    }

    public static Reservas deserialize(String filepath) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(filepath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Reservas reservas = (Reservas) ois.readObject();
        ois.close();
        fis.close();
        return reservas;
    }
    
}
