package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para criação de uma nova Ordem de Serviço")
public record OrdemServicoCreateRequest(

    @Schema(example = "1")
    @NotNull(message = "O ID do cliente é obrigatório")
    Long clienteId,

    @Schema(example = "1")
    @NotNull(message = "O ID do veículo é obrigatório")
    Long veiculoId,

    @Schema(example = "2026-08-20T18:00:00")
    LocalDateTime dataPrevistaEntrega,

    @Schema(example = "Cliente relata barulho no freio dianteiro ao frear")
    String observacoes,

    @Schema(description = "Lista de serviços a serem realizados na OS")
    @Valid
    List<ItemServicoRequest> servicos,

    @Schema(description = "Lista de peças a serem utilizadas na OS")
    @Valid
    List<ItemPecaRequest> pecas

) {

    @Schema(description = "Serviço vinculado à OS")
    public record ItemServicoRequest(

        @Schema(example = "1")
        @NotNull(message = "O ID do serviço é obrigatório")
        Long servicoId,

        @Schema(example = "1")
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade

    ) {}

    @Schema(description = "Peça vinculada à OS")
    public record ItemPecaRequest(

        @Schema(example = "1")
        @NotNull(message = "O ID da peça é obrigatório")
        Long pecaId,

        @Schema(example = "2")
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade

    ) {}
}
