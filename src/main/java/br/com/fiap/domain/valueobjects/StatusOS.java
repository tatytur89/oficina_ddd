package br.com.fiap.domain.valueobjects;

import java.util.Arrays;
import java.util.List;

public enum StatusOS {
    RECEBIDA("Recebida"),
    EM_ANDAMENTO("Em Andamento"),
    AGUARDANDO_APROVACAO("Aguardando Aprovação"),
    APROVADA("Aprovada"),
    EM_EXECUCAO("Em Execução"),
    CONCLUIDA("Concluída"),
    ENTREGUE("Entregue"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusOS(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<StatusOS> proximosStatusValidos() {
        return switch (this) {
            case RECEBIDA -> List.of(EM_ANDAMENTO, CANCELADA);
            case EM_ANDAMENTO -> List.of(AGUARDANDO_APROVACAO, CANCELADA);
            case AGUARDANDO_APROVACAO -> List.of(APROVADA, CANCELADA);
            case APROVADA -> List.of(EM_EXECUCAO, CANCELADA);
            case EM_EXECUCAO -> List.of(CONCLUIDA);
            case CONCLUIDA -> List.of(ENTREGUE);
            case ENTREGUE -> List.of();
            case CANCELADA -> List.of();
        };
    }

    public boolean podeTransicionarPara(StatusOS novoStatus) {
        return proximosStatusValidos().contains(novoStatus);
    }
}
