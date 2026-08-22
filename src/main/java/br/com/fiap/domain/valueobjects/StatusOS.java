package br.com.fiap.domain.valueobjects;

import java.util.List;

public enum StatusOS {
    RECEBIDA("Recebida"),
    EM_DIAGNOSTICO("Em diagnóstico"),
    AGUARDANDO_APROVACAO("Aguardando aprovação"),
    EM_EXECUCAO("Em execução"),
    FINALIZADA("Finalizada"),
    ENTREGUE("Entregue");

    private final String descricao;

    StatusOS(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<StatusOS> proximosStatusValidos() {
        return switch (this) {
            case RECEBIDA -> List.of(EM_DIAGNOSTICO);
            case EM_DIAGNOSTICO -> List.of(AGUARDANDO_APROVACAO);
            case AGUARDANDO_APROVACAO -> List.of(EM_EXECUCAO);
            case EM_EXECUCAO -> List.of(FINALIZADA);
            case FINALIZADA -> List.of(ENTREGUE);
            case ENTREGUE -> List.of();
        };
    }

    public boolean podeTransicionarPara(StatusOS novoStatus) {
        return proximosStatusValidos().contains(novoStatus);
    }
}
