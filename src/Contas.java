import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Contas implements Serializable{
    private HashMap<String,User> contas;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();



    public Contas(){
        contas = new HashMap<>();
    }


    public boolean existe (String username){
        try {
            lock.readLock().lock();
            return contas.containsKey(username);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public void addViagem(String username,Viagem viagem){
        try {
            lock.writeLock().lock();
            User user = this.contas.get(username);
            user.addViagem(viagem);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public List<Viagem> getViagens(String username){
        try {
            lock.readLock().lock();
            User user = this.contas.get(username);
            return user.getViagens();
        }
        finally {
            lock.readLock().unlock();

        }
    }

    public void cancelaViagem(String username, int idViagem)throws Exception{
        try {
            lock.writeLock().lock();
            User user = contas.get(username);
            user.cancelaViagem(idViagem);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void adicionarUser(String username,String password)throws Exception{
        try {
            lock.writeLock().lock();
            if (existe(username)) throw new Exception("Utilizador com nome  \"" + username + "\" já existe");//TODO: Criar Exception
            User user = new User(username,password,true);
            this.contas.put(username, user);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public boolean autenticarUser(String username, String password) throws Exception {
        try {
            lock.readLock().lock();
            if (!contas.containsKey(username)) throw new Exception("ERRO - username não existe");
            User user = contas.get(username);
            String pass = user.getPass();
            if (user.isLoggedIn()) throw new Exception("ERRO - Utilizador com loggin feito");
            if (pass.equals(password)){
                user.logIn();
                return true;
            }
            else return false;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public void logout(String username){
        User user = this.contas.get(username);
        user.logOut();
    }

    public boolean isAdmin (String username){
        try {
            lock.readLock().lock();
            User user = contas.get(username);
            return user instanceof Admin;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public void addAdmin (String username,String password) throws Exception {
        try {
            lock.writeLock().lock();
            Admin admin = new Admin(username, password);
            if (existe(username)) throw new Exception("Já existe");
            this.contas.put(username,admin);
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

    public static Contas deserialize(String filepath) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(filepath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Contas contas = (Contas) ois.readObject();
        ois.close();
        fis.close();
        return contas;
    }
}
