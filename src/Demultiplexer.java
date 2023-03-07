import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Demultiplexer implements AutoCloseable{

    private final Connection conn;
    private final Lock l = new ReentrantLock();
    private final Map<Integer, Entry> buffer = new HashMap<>();
    private IOException exception = null;

    private class Entry{
        int waiters = 0;
        Queue<byte[]> queue = new ArrayDeque<>();
        Condition notEmpty = l.newCondition();
    }

    private Entry get(int tag){
        Entry entry = buffer.get(tag);
        if(entry == null){
            entry = new Entry();
            buffer.put(tag,entry);
        }
        return entry;
    }

    public Demultiplexer(Connection conn) {
        this.conn = conn;
    }

    public void start() {
        new Thread(() -> {
            try{
                while(true){
                    Connection.FrameCliente frame = conn.receiveCliente();
                    l.lock();
                    try{
                        Entry entry = get(frame.tag);
                        entry.queue.add(frame.data);
                        entry.notEmpty.signalAll();
                    } finally {
                        l.unlock();
                    }
                }
            } catch (IOException e){//Caso ocorra um erro(exceção), para o programa não "morrer" acordar as threads adormecidas
                l.lock();
                try{
                    exception = e;
                    buffer.forEach((k,v) -> v.notEmpty.signalAll());
                } finally {
                    l.unlock();
                }
            }
        }).start();
    }

    public void send(Connection.FrameCliente frame) throws IOException {
        this.conn.send(frame);
    }

    public void send(int tag, String username,byte[] data) throws IOException {
        this.conn.send(tag,username,data);
    }

    public void send(int tag, String username) throws IOException {
        this.conn.send(tag,username);
    }


    public void sendViagem(int tag, String username, PercursoCliente percurso) throws IOException {
        this.conn.sendViagem(tag,username,percurso);
    }

    public void sendString (int tag,String username,String data) throws IOException {
        this.conn.sendString(tag,username,data);
    }

    public void sendVoo(int tag,String username,String origem,String destino,int capacidade) throws IOException {
        this.conn.sendVoo(tag,username,origem,destino,capacidade);
    }


    public byte[] receive(int tag) throws IOException, InterruptedException {
        l.lock();
        Entry entry;
        try{
            entry = get(tag);
            entry.waiters++;
            while(true){
                if(!entry.queue.isEmpty()){
                    entry.waiters--;
                    byte[] data = entry.queue.poll();
                    if(entry.waiters == 0 && entry.queue.isEmpty())
                        buffer.remove(tag);
                    return data;
                }
                if(exception != null)//Se foi acordada por um erro, lançar a exceção
                    throw exception;
                entry.notEmpty.await();
            }
        }finally {
            l.unlock();
        }
    }

    public void close() throws IOException {
        conn.close();
    }
}