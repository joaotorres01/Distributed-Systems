import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PercursoCliente {
    List<String>percurso;
    LocalDate inicio;
    LocalDate fim;


    public PercursoCliente(List<String> percurso, LocalDate inicio, LocalDate fim) {
        this.percurso = percurso;
        this.inicio = inicio;
        this.fim = fim;
    }


    public void serialize(DataOutputStream out) throws IOException {
        out.writeInt(percurso.size());
        for (String str : percurso)
            out.writeUTF(str);
        out.writeUTF(inicio.toString());
        out.writeUTF(fim.toString());
    }

    public static PercursoCliente deserialize(DataInputStream in) throws IOException {
        int size = in.readInt();
        List<String> res = new ArrayList<>();
        for (int i = 0; i < size;i++){
            String str = in.readUTF();
            res.add(str);
        }
        LocalDate inicio = LocalDate.parse(in.readUTF());
        LocalDate fim = LocalDate.parse(in.readUTF());
        return new PercursoCliente(res,inicio,fim);
    }

}
