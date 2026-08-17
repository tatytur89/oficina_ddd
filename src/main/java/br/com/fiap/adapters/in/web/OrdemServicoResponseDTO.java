package br.com.fiap.adapters.in.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.fiap.domain.entities.OrdemServico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma Ordem de Serviço")
public class OrdemServicoResponseDTO {

    @Schema(
        description = "Identificador único da OS",
        example = "1"
    )
    private Long id;

    @Schema(
        description = "ID do cliente proprietário",
        example = "1"
    )
    private Long clienteId;

    @Schema(
        description = "ID do veículo atendido",
        example = "1"
    )
    private Long veiculoId;

    @Schema(
        description = "Status atual da OS na máquina de estados",
        example = "RECEBIDA",
        allowableValues = {"RECEBIDA", "EM_ANDAMENTO", "AGUARDANDO_APROVACAO", "APROVADA", "EM_EXECUCAO", "CONCLUIDA", "ENTREGUE", "CANCELADA"}
    )
    private String status;

    @Schema(
        description = "Data e hora da abertura da OS",
        example = "2026-08-12T10:30:00",
        format = "date-time"
    )
    private LocalDateTime dataAbertura;

    @Schema(
        description = "Data prevista de entrega",
        example = "2026-08-20T18:00:00",
        format = "date-time"
    )
    private LocalDateTime dataPrevistaEntrega;

    @Schema(
        description = "Data e hora da conclusão do serviço",
        example = "2026-08-18T16:45:00",
        format = "date-time"
    )
    private LocalDateTime dataConclusao;

    @Schema(
        description = "Observações gerais sobre o atendimento",
        example = "Cliente relata barulho no freio dianteiro ao frear"
    )
    private String observacoes;

    @Schema(
        description = "Valor total dos serviços em reais (R$)",
        example = "250.00"
    )
    private BigDecimal valorServicos;

    @Schema(
        description = "Valor total das peças em reais (R$)",
        example = "91.80"
    )
    private BigDecimal valorPecas;

    @Schema(
        description = "Valor total da OS (serviços + peças) em reais (R$)",
        example = "341.80"
    )
    private BigDecimal valorTotal;

    @Schema(
        description = "Lista de serviços vinculados à OS"
    )
    private List<ServicoOSResponseDTO> servicos;

    @Schema(
        description = "Lista de peças utilizadas na OS"
    )
    private List<PecaOSResponseDTO> pecas;

    public OrdemServicoResponseDTO(OrdemServico os) {
        this.id = os.getId();
        this.clienteId = os.getClienteId();
        this.veiculoId = os.getVeiculoId();
        this.status = os.getStatus().name();
        this.dataAbertura = os.getDataAbertura();
        this.dataPrevistaEntrega = os.getDataPrevistaEntrega();
        this.dataConclusao = os.getDataConclusao();
        this.observacoes = os.getObservacoes();
        this.valorServicos = os.getValorServicos().getValor();
        this.valorPecas = os.getValorPecas().getValor();
        this.valorTotal = os.getValorTotal().getValor();
        this.servicos = os.getServicos().stream()
            .map(ServicoOSResponseDTO::new)
            .toList();
        this.pecas = os.getPecas().stream()
            .map(PecaOSResponseDTO::new)
            .toList();
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
    public List<ServicoOSResponseDTO> getServicos() { return servicos; }
    public List<PecaOSResponseDTO> getPecas() { return pecas; }

    @Schema(description = "Dados de um serviço vinculado à OS")
    public static class ServicoOSResponseDTO {

        @Schema(description = "ID do serviço", example = "1")
        private Long servicoId;

        @Schema(description = "Nome do serviço", example = "Troca de Óleo")
        private String nomeServico;

        @Schema(description = "Quantidade executada", example = "1")
        private int quantidade;

        @Schema(description = "Preço unitário do serviço", example = "150.00")
        private BigDecimal precoUnitario;

        @Schema(description = "Valor total do item (preço x quantidade)", example = "150.00")
        private BigDecimal valorTotal;

        public ServicoOSResponseDTO(br.com.fiap.domain.entities.ServicoOS servico) {
            this.servicoId = servico.getServicoId();
            this.nomeServico = servico.getNomeServico();
            this.quantidade = servico.getQuantidade();
            this.precoUnitario = servico.getPrecoUnitario().getValor();
            this.valorTotal = servico.getValorTotal().getValor();
        }

        public Long getServicoId() { return servicoId; }
        public String getNomeServico() { return nomeServico; }
        public int getQuantidade() { return quantidade; }
        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public BigDecimal getValorTotal() { return valorTotal; }
    }

    @Schema(description = "Dados de uma peça utilizada na OS")
    public static class PecaOSResponseDTO {

        @Schema(description = "ID da peça", example = "1")
        private Long pecaId;

        @Schema(description = "Nome da peça", example = "Filtro de Óleo")
        private String nomePeca;

        @Schema(description = "Código da peça", example = "FIL001")
        private String codigoPeca;

        @Schema(description = "Quantidade utilizada", example = "2")
        private int quantidade;

        @Schema(description = "Preço unitário da peça", example = "45.90")
        private BigDecimal precoUnitario;

        @Schema(description = "Valor total do item (preço x quantidade)", example = "91.80")
        private BigDecimal valorTotal;

        public PecaOSResponseDTO(br.com.fiap.domain.entities.PecaOS peca) {
            this.pecaId = peca.getPecaId();
            this.nomePeca = peca.getNomePeca();
            this.codigoPeca = peca.getCodigoPeca();
            this.quantidade = peca.getQuantidade();
            this.precoUnitario = peca.getPrecoUnitario().getValor();
            this.valorTotal = peca.getValorTotal().getValor();
        }

        public Long getPecaId() { return pecaId; }
        public String getNomePeca() { return nomePeca; }
        public String getCodigoPeca() { return codigoPeca; }
        public int getQuantidade() { return quantidade; }
        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public BigDecimal getValorTotal() { return valorTotal; }
    }
}
