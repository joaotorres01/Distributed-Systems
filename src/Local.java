import java.io.Serializable;
import java.util.*;

public class Local implements Serializable {
    private String nome;
    private Set<Local> destinosPossiveis;


    public Local(String nome){
        this.nome = nome;
        this.destinosPossiveis = new HashSet<>();
    }

    public String getNome() {
        return nome;
    }

    public void adicionarDestino(Local local){
        destinosPossiveis.add(local);
    }

    public boolean containsDestino(Local local){
        return destinosPossiveis.contains(local);
    }


    public List<List<String>> allPercursos(Local local){
        List<List<String>> res = new ArrayList<>();
        if(destinosPossiveis.contains(local)){
            res.add(Arrays.asList(nome, local.nome));
        }
        for (Local l : destinosPossiveis){
            if (!l.equals(local)) {
                if (l.destinosPossiveis.contains(local)) res.add(Arrays.asList(nome , l.nome ,local.nome));
                for (Local l2 : l.destinosPossiveis) {
                    if (!l2.equals(local)) {
                        if (l2.destinosPossiveis.contains(local))
                            res.add(Arrays.asList(nome , l.nome , l2.nome ,local.nome));
                    }
                }
            }
        }
        return res;
    }
}

