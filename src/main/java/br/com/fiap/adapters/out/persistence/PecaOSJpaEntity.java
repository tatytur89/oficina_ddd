package br.com.fiap.adapters.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pecas_os")
public class PecaOSJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pecaId;

    private String nomePeca;

    private String codigoPeca;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private BigDecimal valorTotal;

    public PecaOSJpaEntity() {}

    public PecaOSJpaEntity(Long id, Long pecaId, String nomePeca, String codigoPeca, int quantidade, BigDecimal precoUnitario, BigDecimal valorTotal) {
        this.id = id;
        this.pecaId = pecaId;
        this.nomePeca = nomePeca;
        this.codigoPeca = codigoPeca;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.valorTotal = valorTotal;
    }

    public Long getId() { return id; }
    public Long getPecaId() { return pecaId; }
    public String getNomePeca() { return nomePeca; }
    public String getCodigoPeca() { return codigoPeca; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getValorTotal() { return valorTotal; }
}
