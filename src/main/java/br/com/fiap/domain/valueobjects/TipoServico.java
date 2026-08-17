package br.com.fiap.domain.valueobjects;

public enum TipoServico {
    REVISAO("Revisão"),
    MANUTENCAO("Manutenção"),
    TROCA_PECA("Troca de Peça"),
    ALINHAMENTO("Alinhamento"),
    BALANCEAMENTO("Balanceamento"),
    MECANICA_GERAL("Mecânica Geral"),
    ELETRICA("Elétrica"),
    SUSPENSAO("Suspensão"),
    FREIOS("Freios");

    private final String descricao;

    TipoServico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
