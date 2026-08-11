package br.com.fiap.domain.valueobjects;

public enum TipoDocumento {
    CPF("CPF"),
    CNPJ("CNPJ");

    private final String descricao;

    TipoDocumento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
