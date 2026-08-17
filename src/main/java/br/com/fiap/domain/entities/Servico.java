package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;

public class Servico {
    private Long id;
    private String nome;
    private String descricao;
    private Preco preco;
    private TipoServico tipo;
    private Integer tempoEstimadoMinutos;

    public Servico(Long id, String nome, String descricao, Preco preco, TipoServico tipo, Integer tempoEstimadoMinutos) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.tipo = tipo;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
    }

    public Servico(Long id, String nome, String descricao, double preco, String tipo, Integer tempoEstimadoMinutos) {
        this(id, nome, descricao, new Preco(preco), TipoServico.valueOf(tipo), tempoEstimadoMinutos);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Preco getPreco() { return preco; }
    public TipoServico getTipo() { return tipo; }
    public Integer getTempoEstimadoMinutos() { return tempoEstimadoMinutos; }
}
