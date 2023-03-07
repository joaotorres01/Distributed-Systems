import com.sun.org.glassfish.external.statistics.annotations.Reset;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import jdk.nashorn.internal.runtime.ECMAException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.RuleBasedCollator;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Servidor {

    private static final String reservasFile = "reservas.ser";
    private static final String contasFile = "contas.ser";

    public static void main(String[] args) {
        try {
            Reservas reservas;
            if (!Files.exists(Paths.get(contasFile))) {
                reservas = new Reservas();
                reservas.adicionarVoo("Porto", "Lisboa", 1);
                reservas.adicionarVoo("Porto", "Paris", 500);
                reservas.adicionarVoo("Lisboa", "Paris", 500);
                reservas.adicionarVoo("Paris", "Madrid", 500);
                reservas.adicionarVoo("Porto", "Madrid", 500);
                reservas.adicionarVoo("Lisboa", "Londres", 500);
                reservas.adicionarVoo("Londres", "Paris", 500);
                reservas.adicionarVoo("Paris", "Londres", 500);
                reservas.adicionarVoo("Londres", "Porto", 500);
            }
            else{
                reservas = Reservas.deserialize(reservasFile);
            }



            Contas contas;
            if (!Files.exists(Paths.get(contasFile))){
                contas = new Contas();
                contas.addAdmin("diogo", "123");
                contas.serialize("contas.ser");
            }
            else contas = Contas.deserialize(contasFile);
            ServerSocket ss = new ServerSocket(12345);

            while (true) {
                Socket socket = ss.accept();
                Connection connection = new Connection(socket);
                Worker worker = new Worker(connection,contas,reservas);
                new Thread(worker).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class Worker implements Runnable {
        private final Connection connection;
        private final Contas contas;
        private final Reservas reservas;
        private final ReentrantLock lock = new ReentrantLock();

        public Worker(Connection connection,Contas contas, Reservas reservas){
            this.connection = connection;
            this.contas = contas;
            this.reservas = reservas;
        }


        public void run() {
            boolean out = false;
            String username = null;
            while (!out) {
                try {
                    Connection.FrameServidor frame = connection.receive();
                    int tag = frame.tag;
                    username = frame.username;
                    if (tag == Constantes.iniciarSessao) {

                        String password = (String) frame.data;
                        try {
                            boolean loginValido = contas.autenticarUser(username, password);
                            if (loginValido) {
                                String message = contas.isAdmin(username) ? "OK_ADMIN" : "OK_USER";
                                connection.send(tag, message.getBytes());
                            }
                            else connection.send(tag,  "ERRO - Palavra passe errada".getBytes());
                        }
                        catch (Exception e){
                            connection.send(tag,e.getMessage().getBytes());
                        }
                    }

                    if (tag == Constantes.criarConta) {
                        String password = (String) frame.data;
                        try {
                            contas.adicionarUser(username, password);
                            connection.send(tag,"OK".getBytes());
                        }
                        catch (Exception e){
                            connection.send(tag,"Já existe um utilizador com esse username".getBytes());
                        }
                    }

                    if (tag == Constantes.reservarViagem){
                        PercursoCliente percursoCliente = (PercursoCliente) frame.data;
                        List<String> listDestinos = percursoCliente.percurso;
                        if (reservas.validarDestinos(listDestinos)){
                            try {
                                Viagem viagem = reservas.marcarViagem(listDestinos,percursoCliente.inicio,percursoCliente.fim);
                                connection.send(tag,String.valueOf(viagem.getIdReserva()).getBytes());
                                contas.addViagem(username,viagem);
                                System.out.println(viagem);
                            }
                            catch (Exception e){
                                connection.send(tag,e.getMessage().getBytes());
                            }
                        }
                        else{
                            connection.send(tag,"ERRO - Destino(s) inválido(s)".getBytes());
                        }

                    }

                    if (tag == Constantes.cancelarViagem){
                        int id = Integer.parseInt((String) frame.data);
                        try {
                            contas.cancelaViagem(username, id);
                            connection.send(tag,  "OK".getBytes());
                        }
                        catch (Exception e){
                            connection.send(tag,  e.getMessage().getBytes());
                        }
                    }
                    if (tag == Constantes.voosExistentes){
                        List<Voo> voosDiarios = reservas.getVoosDiarios();
                        if(voosDiarios.size()==0) connection.send(tag,  "Lista Vazia".getBytes());
                        else {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ObjectOutputStream oos = new ObjectOutputStream(baos);
                            oos.writeInt(voosDiarios.size());
                            for (Voo voo : voosDiarios){
                                String origem = voo.getOrigem();
                                String destino = voo.getDestino();
                                oos.writeUTF(origem);
                                oos.writeUTF(destino);
                            }
                            oos.flush();
                            byte[] bytes= baos.toByteArray();
                            connection.send(tag, bytes);
                        }
                    }

                    if (tag == Constantes.inserirVoo){
                        Voo voo = (Voo)frame.data;
                        try {
                            reservas.adicionarVoo(voo);
                            connection.send(tag, "OK".getBytes());
                        }
                        catch (Exception e){
                            connection.send(tag,e.getMessage().getBytes());
                        }
                    }
                    if (tag == Constantes.encerrarDia){
                        LocalDate date = (LocalDate) frame.data;
                        reservas.cancelarDia(date);
                    }
                    if (tag == Constantes.viagensReservadas){
                        List<Viagem> viagens = contas.getViagens(username);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeInt(viagens.size());
                        for (Viagem viagem : viagens){
                            oos.writeInt(viagem.getIdReserva());
                            oos.writeUTF(viagem.getEstadoString());
                            oos.writeUTF(viagem.getDia().toString());
                            List<Voo> voos = viagem.getVoos();
                            oos.writeInt(voos.size());
                            for (Voo voo : voos){
                                String origem = voo.getOrigem();
                                String destino = voo.getDestino();
                                oos.writeUTF(origem);
                                oos.writeUTF(destino);
                            }
                        }
                        oos.flush();
                        byte[] bytes= baos.toByteArray();
                        connection.send(tag, bytes);
                    }
                    if (tag == Constantes.allPercursos){
                        //String origem = username;
                        String destino = (String) frame.data;
                        List<List<String>> allCaminhos = reservas.getAllCaminhos(username,destino);
                        Map<String,Short> mapLocais = new HashMap<>();
                        short size = 0;
                        for (List<String> caminho : allCaminhos){
                            for (String local : caminho){
                                if (!mapLocais.containsKey(local)) mapLocais.put(local,size++);
                            }
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);

                        oos.writeShort(mapLocais.size());
                        for (Map.Entry<String,Short> entry : mapLocais.entrySet()){
                            oos.writeShort(entry.getValue());
                            oos.writeUTF(entry.getKey());
                        }

                        oos.writeShort(allCaminhos.size());
                        for (List<String> caminho : allCaminhos){
                            oos.writeShort(caminho.size());
                            for (String local : caminho){
                                oos.writeShort(mapLocais.get(local));
                            }
                        }
                        oos.flush();

                        byte[]bytes = baos.toByteArray();
                        connection.send(tag,bytes);
                    }
                    if (tag == Constantes.terminarSessao){
                        contas.logout(frame.username);
                        writeFile(contas,reservas);
                        out = true;
                    }
                }catch(EOFException ignored){
                    if (username != null)contas.logout(username);
                    writeFile(contas,reservas);
                    out = true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void writeFile(Contas contas, Reservas reservas) {
            try {
                lock.lock();
                contas.serialize(contasFile);
                reservas.serialize(reservasFile);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }


}
