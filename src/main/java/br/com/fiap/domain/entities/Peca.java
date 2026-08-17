package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Preco;

public class Peca {
    private Long id;
    private String nome;
    private String descricao;
    private String codigo;
    private Preco preco;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;

    public Peca(Long id, String nome, String descricao, String codigo, Preco preco, Integer quantidadeEstoque, Integer estoqueMinimo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.codigo = codigo;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.estoqueMinimo = estoqueMinimo;
    }

    public Peca(Long id, String nome, String descricao, String codigo, double preco, Integer quantidadeEstoque, Integer estoqueMinimo) {
        this(id, nome, descricao, codigo, new Preco(preco), quantidadeEstoque, estoqueMinimo);
    }

    public void baixarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade a ser baixada deve ser maior que zero.");
        }
        if (this.quantidadeEstoque < quantidade) {
            throw new IllegalStateException(
                String.format("Estoque insuficiente. Disponível: %d, Solicitado: %d", 
                    this.quantidadeEstoque, quantidade)
            );
        }
        this.quantidadeEstoque -= quantidade;
    }

    public void reporEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade a ser reposta deve ser maior que zero.");
        }
        this.quantidadeEstoque += quantidade;
    }

    public boolean estoqueBaixo() {
        return this.quantidadeEstoque <= this.estoqueMinimo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCodigo() { return codigo; }
    public Preco getPreco() { return preco; }
    public Integer getQuantidadeEstoque() { return quantidadeEstoque; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
}
