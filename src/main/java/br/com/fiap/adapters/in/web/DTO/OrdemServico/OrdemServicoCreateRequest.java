package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para criação de uma nova Ordem de Serviço")
public record OrdemServicoCreateRequest(

    @Schema(example = "1")
    @NotNull(message = "O ID do cliente é obrigatório")
    Long clienteId,

    @Schema(example = "1")
    @NotNull(message = "O ID do veículo é obrigatório")
    Long veiculoId,

    @Schema(example = "Cliente relata barulho no freio dianteiro ao frear")
    String observacoes

) {}
