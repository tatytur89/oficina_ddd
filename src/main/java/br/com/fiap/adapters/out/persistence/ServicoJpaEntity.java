package br.com.fiap.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicos")
public class ServicoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private java.math.BigDecimal preco;

    @Column(nullable = false)
    private String tipo;

    private Integer tempoEstimadoMinutos;

    public ServicoJpaEntity() {}

    public ServicoJpaEntity(Long id, String nome, String descricao, java.math.BigDecimal preco, String tipo, Integer tempoEstimadoMinutos) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.tipo = tipo;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public java.math.BigDecimal getPreco() { return preco; }
    public String getTipo() { return tipo; }
    public Integer getTempoEstimadoMinutos() { return tempoEstimadoMinutos; }
}
