package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Preco;

public class ServicoOS {
    private Long servicoId;
    private String nomeServico;
    private int quantidade;
    private Preco precoUnitario;
    private Preco valorTotal;

    public ServicoOS(Long servicoId, String nomeServico, int quantidade, Preco precoUnitario, Preco valorTotal) {
        this.servicoId = servicoId;
        this.nomeServico = nomeServico;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.valorTotal = valorTotal;
    }

    public Long getServicoId() { return servicoId; }
    public String getNomeServico() { return nomeServico; }
    public int getQuantidade() { return quantidade; }
    public Preco getPrecoUnitario() { return precoUnitario; }
    public Preco getValorTotal() { return valorTotal; }
}
