package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Preco;

public class PecaOS {
    private Long pecaId;
    private String nomePeca;
    private String codigoPeca;
    private int quantidade;
    private Preco precoUnitario;
    private Preco valorTotal;

    public PecaOS(Long pecaId, String nomePeca, String codigoPeca, int quantidade, Preco precoUnitario, Preco valorTotal) {
        this.pecaId = pecaId;
        this.nomePeca = nomePeca;
        this.codigoPeca = codigoPeca;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.valorTotal = valorTotal;
    }

    public Long getPecaId() { return pecaId; }
    public String getNomePeca() { return nomePeca; }
    public String getCodigoPeca() { return codigoPeca; }
    public int getQuantidade() { return quantidade; }
    public Preco getPrecoUnitario() { return precoUnitario; }
    public Preco getValorTotal() { return valorTotal; }
}
