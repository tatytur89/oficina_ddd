package br.com.fiap.adapters.in.web;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para criação de uma nova Ordem de Serviço")
public class OrdemServicoRequestDTO {

    @Schema(
        description = "ID do cliente proprietário (deve existir no cadastro)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O ID do cliente é obrigatório")
    private Long clienteId;

    @Schema(
        description = "ID do veículo a ser atendido (deve existir no cadastro)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O ID do veículo é obrigatório")
    private Long veiculoId;

    @Schema(
        description = "Data prevista de entrega para o cliente",
        example = "2026-08-20T18:00:00",
        format = "date-time"
    )
    private LocalDateTime dataPrevistaEntrega;

    @Schema(
        description = "Observações gerais sobre o atendimento ou defeito relatado",
        example = "Cliente relata barulho no freio dianteiro ao frear"
    )
    private String observacoes;

    @Schema(
        description = "Lista de serviços a serem realizados na OS"
    )
    private List<ServicoOSRequestDTO> servicos;

    @Schema(
        description = "Lista de peças a serem utilizadas na OS"
    )
    private List<PecaOSRequestDTO> pecas;

    public OrdemServicoRequestDTO() {}

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long veiculoId) { this.veiculoId = veiculoId; }

    public LocalDateTime getDataPrevistaEntrega() { return dataPrevistaEntrega; }
    public void setDataPrevistaEntrega(LocalDateTime dataPrevistaEntrega) { this.dataPrevistaEntrega = dataPrevistaEntrega; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public List<ServicoOSRequestDTO> getServicos() { return servicos; }
    public void setServicos(List<ServicoOSRequestDTO> servicos) { this.servicos = servicos; }

    public List<PecaOSRequestDTO> getPecas() { return pecas; }
    public void setPecas(List<PecaOSRequestDTO> pecas) { this.pecas = pecas; }

    @Schema(description = "Dados de um serviço vinculado à OS")
    public static class ServicoOSRequestDTO {

        @Schema(
            description = "ID do serviço (deve existir no catálogo)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "O ID do serviço é obrigatório")
        private Long servicoId;

        @Schema(
            description = "Quantidade deste serviço a ser executada",
            example = "1",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "A quantidade é obrigatória")
        private Integer quantidade;

        public ServicoOSRequestDTO() {}

        public Long getServicoId() { return servicoId; }
        public void setServicoId(Long servicoId) { this.servicoId = servicoId; }

        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    }

    @Schema(description = "Dados de uma peça vinculada à OS")
    public static class PecaOSRequestDTO {

        @Schema(
            description = "ID da peça (deve existir no estoque)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "O ID da peça é obrigatório")
        private Long pecaId;

        @Schema(
            description = "Quantidade desta peça a ser utilizada",
            example = "2",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "A quantidade é obrigatória")
        private Integer quantidade;

        public PecaOSRequestDTO() {}

        public Long getPecaId() { return pecaId; }
        public void setPecaId(Long pecaId) { this.pecaId = pecaId; }

        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    }
}
