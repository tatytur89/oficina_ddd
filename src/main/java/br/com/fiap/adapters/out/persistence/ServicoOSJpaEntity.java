package br.com.fiap.adapters.out.persistence;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicos_os")
public class ServicoOSJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long servicoId;

    private String nomeServico;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private BigDecimal valorTotal;

    public ServicoOSJpaEntity() {}

    public ServicoOSJpaEntity(Long id, Long servicoId, String nomeServico, int quantidade, BigDecimal precoUnitario, BigDecimal valorTotal) {
        this.id = id;
        this.servicoId = servicoId;
        this.nomeServico = nomeServico;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.valorTotal = valorTotal;
    }

    public Long getId() { return id; }
    public Long getServicoId() { return servicoId; }
    public String getNomeServico() { return nomeServico; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getValorTotal() { return valorTotal; }
}
