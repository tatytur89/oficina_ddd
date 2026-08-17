package br.com.fiap.adapters.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordens_servico")
public class OrdemServicoJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Long veiculoId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    private LocalDateTime dataPrevistaEntrega;

    private LocalDateTime dataConclusao;

    private String observacoes;

    @Column(nullable = false)
    private BigDecimal valorServicos;

    @Column(nullable = false)
    private BigDecimal valorPecas;

    @Column(nullable = false)
    private BigDecimal valorTotal;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id")
    private List<ServicoOSJpaEntity> servicos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id")
    private List<PecaOSJpaEntity> pecas = new ArrayList<>();

    public OrdemServicoJpaEntity() {}

    public OrdemServicoJpaEntity(Long id, Long clienteId, Long veiculoId, String status,
                                  LocalDateTime dataAbertura, LocalDateTime dataPrevistaEntrega,
                                  LocalDateTime dataConclusao, String observacoes,
                                  BigDecimal valorServicos, BigDecimal valorPecas, BigDecimal valorTotal) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = status;
        this.dataAbertura = dataAbertura;
        this.dataPrevistaEntrega = dataPrevistaEntrega;
        this.dataConclusao = dataConclusao;
        this.observacoes = observacoes;
        this.valorServicos = valorServicos;
        this.valorPecas = valorPecas;
        this.valorTotal = valorTotal;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public Long getVeiculoId() { return veiculoId; }
    public String getStatus() { return status; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public LocalDateTime getDataPrevistaEntrega() { return dataPrevistaEntrega; }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public String getObservacoes() { return observacoes; }
    public BigDecimal getValorServicos() { return valorServicos; }
    public BigDecimal getValorPecas() { return valorPecas; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public List<ServicoOSJpaEntity> getServicos() { return servicos; }
    public List<PecaOSJpaEntity> getPecas() { return pecas; }

    public void setStatus(String status) { this.status = status; }
    public void setDataConclusao(LocalDateTime dataConclusao) { this.dataConclusao = dataConclusao; }
    public void setValorServicos(BigDecimal valorServicos) { this.valorServicos = valorServicos; }
    public void setValorPecas(BigDecimal valorPecas) { this.valorPecas = valorPecas; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public void setServicos(List<ServicoOSJpaEntity> servicos) { this.servicos = servicos; }
    public void setPecas(List<PecaOSJpaEntity> pecas) { this.pecas = pecas; }
}
