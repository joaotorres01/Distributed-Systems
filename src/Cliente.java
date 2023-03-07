import javax.swing.plaf.synth.SynthOptionPaneUI;
import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Cliente {

    private static void pressAnyKeyToContinue()
    {
        System.out.println("Enter para continuar");
        try
        {
            System.in.read();
        }
        catch(Exception e)
        {}
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void prompt(){
        System.out.print(">");
    }

    private static void menuCliente (Demultiplexer demultiplexer, String username){
        boolean out = false;
        while (!out){
            System.out.println("MENU CLIENTE");
            System.out.println("1.Reservar viagem");
            System.out.println("2.Cancelar viagem");
            System.out.println("3.Voos Existentes");
            System.out.println("4.Ver viagens reservadas");
            System.out.println("5.Todos percursos");
            System.out.println("6.Terminar Sessão");
            System.out.println("Escolha uma opção");
            prompt();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String opcao = null;
            try {
                opcao = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                int tag;
                switch (opcao) {
                    case "1": {
                        tag = Constantes.reservarViagem;
                        //Reservar viagem
                        System.out.print("Origem:");
                        String origem = in.readLine();
                        List<String> listVoos = new ArrayList<>();
                        listVoos.add(origem);
                        while (true){
                            System.out.print("Próximo Destino:");
                            String destino = in.readLine();
                            if (!listVoos.contains(destino)){
                                listVoos.add(destino);
                            }
                            else System.out.println("INVÁLIDO - Repetido");
                            System.out.println("Quer adicionar mais locais?");
                            System.out.println("0.Não");
                            System.out.println("1.Sim");
                            prompt();
                            String ans = in.readLine();
                            if (ans.equals("0"))break;
                        }
                        System.out.println("INTERVALO DE DATAS");
                        LocalDate inicio = askData(in,"Inicio:");
                        LocalDate fim;
                        do {
                            fim = askData(in,"Fim:");
                        }while (fim.isBefore(inicio));
                        PercursoCliente percursoCliente = new PercursoCliente(listVoos,inicio,fim);
                        demultiplexer.sendViagem(tag,username,percursoCliente);
                        String resposta = new String(demultiplexer.receive(tag));
                        if (resposta.startsWith("ERRO")){
                            System.out.println(resposta);
                        }
                        else System.out.println("ID da Reserva ->" + resposta);

                        break;
                    }
                    case "2": {
                        tag = Constantes.cancelarViagem;
                        //Cancelar Viagem
                        System.out.print("Id Viagem:");
                        String id = in.readLine();
                        demultiplexer.sendString(tag, username, id);
                        String resposta = new String(demultiplexer.receive(tag));
                        if (resposta.startsWith("ERRO"))
                            System.out.println(resposta);
                        else System.out.println("Viagem cancelada com sucesso");
                        break;
                    }
                    case "3": {
                        tag = Constantes.voosExistentes;
                        //Voos Existentes
                        demultiplexer.send(tag, username);
                        byte[]array = demultiplexer.receive(tag);
                        ByteArrayInputStream bis = new ByteArrayInputStream(array);
                        ObjectInput input = new ObjectInputStream(bis);
                        int len = input.readInt();
                        System.out.println("VOOS EXISTENTES");
                        for (int i = 0; i < len;i++){
                            String origem = input.readUTF();
                            String destino = input.readUTF();
                            System.out.println(origem +"->" + destino);
                        }
                        break;
                    }
                    case "4":{
                        tag = Constantes.viagensReservadas;
                        //Viagens Reservadas
                        demultiplexer.send(tag,username);
                        byte[]array = demultiplexer.receive(tag);
                        ByteArrayInputStream bis = new ByteArrayInputStream(array);
                        ObjectInput input = new ObjectInputStream(bis);
                        int len = input.readInt();
                        for (int i = 0; i < len;i++){
                            int idReserva = input.readInt();
                            String estado = input.readUTF();
                            LocalDate date = LocalDate.parse(input.readUTF());
                            int nrVoos = input.readInt();
                            System.out.println("Id da Reserva:" + idReserva);
                            System.out.println("Data:" + date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu")));
                            System.out.println("Estado:" + estado);
                            System.out.print("Voos:{");
                            for (int u = 0; u < nrVoos; u++) {
                                String origem = input.readUTF();
                                String destino = input.readUTF();
                                System.out.print(origem + "->" + destino);
                                if (u < nrVoos -1) System.out.print(",");
                            }
                            System.out.println("}");
                            if(i < len - 1) System.out.println("----------------------");
                        }
                        break;
                    }
                    case "5":{
                        tag = Constantes.allPercursos;
                        //Todos os percursos
                        System.out.print("Origem:");
                        String origem = in.readLine();
                        System.out.print("Destino:");
                        String destino = in.readLine();
                        demultiplexer.sendString(tag,origem,destino);
                        byte[]array = demultiplexer.receive(tag);
                        ByteArrayInputStream bis = new ByteArrayInputStream(array);
                        ObjectInput input = new ObjectInputStream(bis);
                        short lenLocais = input.readShort();
                        Map<Short,String> mapLocais = new HashMap<>();
                        for (short i = 0; i < lenLocais;i++){
                            short num = input.readShort();
                            String local = input.readUTF();
                            mapLocais.put(num,local);
                        }
                        short lenPercurso = input.readShort();
                        for (short i = 0; i < lenPercurso;i++){
                            short len =  input.readShort();
                            for (short j = 0; j < len;j++){
                                short num = input.readShort();
                                String value = mapLocais.get(num);
                                System.out.print(value);
                                if (j < len -1 ) System.out.print("-");
                            }
                            System.out.println();
                        }

                        break;

                    }
                    case "6": {
                        tag = Constantes.terminarSessao;
                        //Terminar Sessão
                        demultiplexer.send(tag,username);
                        out = true;
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            if(!out) pressAnyKeyToContinue();
            clearScreen();
        }

    }

    //0 - Iniciar Sessao
    //1 - Criar Conta
    //2 - Reservar Viagem
    //3 - Cancelar Viagem
    //4 - Voos Existentes
    //5 - Inserir Voo
    //6 - Encerrar Dia
    //7 - Ver Viagens Reservadas
    //8 - Todos os percursos
    //10- Terminar Sessão


    private static LocalDate askData(BufferedReader in,String question) throws IOException {
        while (true) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-uuuu");
            System.out.print(question);
            System.out.println("Escreva o dia(dd-mm-aaaa)");
            prompt();
            String dateStr = in.readLine();
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                if(date.isAfter(LocalDate.now() ) || date.isEqual(LocalDate.now())) return date;
            } catch (DateTimeParseException ignored) {
            }
            System.out.println("ERRO: DATA INVÁLIDA");
        }
    }

    private static int getInt (BufferedReader in,String question){
        while (true){
            System.out.print(question);
            try {
                return Integer.parseInt(in.readLine());
            } catch (IOException | NumberFormatException ignored) {
            }
        }
    }

    public static void menuAdmin (Demultiplexer demultiplexer,String username){
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String opcao;
        boolean out = false;
        while (!out){
            System.out.println("Menu ADMIN");
            System.out.println("1.Inserir voo");
            System.out.println("2.Encerrar dia");
            System.out.println("3.Terminar sessão");
            System.out.println("Escolha uma opção");
            prompt();
            try {
                int tag;
                opcao = in.readLine();
                switch (opcao) {
                    case "1": {
                        tag = Constantes.inserirVoo;
                        //INSERIR VOO
                        System.out.println("INSERIR VOO");
                        System.out.print("Origem:");
                        String origem = in.readLine();
                        System.out.print("Destino:");
                        String destino = in.readLine();
                        int capacidade = getInt(in,"Capacidade:");
                        demultiplexer.sendVoo(tag, username, origem, destino, capacidade);
                        String resposta = new String(demultiplexer.receive(tag));
                        if (resposta.startsWith("ERRO")) System.out.println(resposta);
                        else System.out.println("Voo adicionado com sucesso");

                        break;
                    }
                    case "2": {
                        tag = Constantes.encerrarDia;
                        //Encerrar dia
                        System.out.println("ENCERRAR DIA");
                        LocalDate date = askData(in,"Data");
                        demultiplexer.send(6,date.toString());
                        System.out.println(date.toString());
                        break;
                    }
                    case "3": {
                        tag = Constantes.terminarSessao;
                        //Terminar sessao
                        demultiplexer.send(10,username);
                        out = true;
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            if(!out) pressAnyKeyToContinue();
            clearScreen();
        }
    }


    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);
            Demultiplexer demultiplexer = new Demultiplexer(new Connection(socket));
            demultiplexer.start();
            String opcao;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            boolean loggedIn = false;
            boolean isAdmin = false;
            String username = null;

            while (true) {
                if (!loggedIn) {
                    System.out.println("LOGIN");
                    System.out.println("1.Inicar Sessão");
                    System.out.println("2.Criar Conta");
                    prompt();
                    opcao = in.readLine();
                    int tag;
                    switch (opcao) {
                        case "1": {
                            tag = Constantes.iniciarSessao;
                            //Iniciar Sessão
                            clearScreen();
                            System.out.println("INICIAR SESSÃO");
                            System.out.print("Username:");
                            username = in.readLine();
                            System.out.print("Password:");
                            String pass = in.readLine();
                            demultiplexer.sendString(tag,username,pass);
                            String resposta = new String(demultiplexer.receive(tag));
                            if (!resposta.startsWith("ERRO")) {
                                loggedIn = true;
                                if (resposta.equals("OK_ADMIN")) isAdmin = true;
                                System.out.println("LOGIN VÁLIDO");
                            } else System.out.println(resposta);
                            break;
                        }
                        case "2": {
                            tag = Constantes.criarConta;
                            //CRIAR CONTA
                            clearScreen();
                            System.out.println("CRIAR CONTA");
                            System.out.print("Introduza o username:");
                            username = in.readLine();
                            System.out.print("Introduza a password:");
                            String pass = in.readLine();
                            demultiplexer.sendString(tag,username,pass);
                            String resposta = new String(demultiplexer.receive(tag));
                            if (resposta.equals("OK")){
                                loggedIn = true;
                                System.out.println("Conta criada com sucesso");
                            }
                            else System.out.println(resposta);
                        }
                    }
                }
                else {
                    if (isAdmin) menuAdmin(demultiplexer,username);
                    else menuCliente(demultiplexer,username);
                    break;
                }
                pressAnyKeyToContinue();
                clearScreen();
            }
            System.out.println("GOODBYE");
            
            //socket.shutdownOutput();
            //socket.shutdownInput();
            //socket.close();
            demultiplexer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
