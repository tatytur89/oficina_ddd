package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import java.util.List;

@Schema(description = "Dados necessários para criação de uma nova Ordem de Serviço")
public record OrdemServicoCreateRequest(

    @Schema(example = "1")
    @NotNull(message = "O ID do cliente é obrigatório")
    @Positive
    Long clienteId,

    @Schema(example = "1")
    @NotNull(message = "O ID do veículo é obrigatório")
    @Positive
    Long veiculoId,

    @Schema(example = "Cliente relata barulho no freio dianteiro ao frear")
    String observacoes,

    @Valid List<@NotNull AdicionarServicoRequest> servicos,
    @Valid List<@NotNull AdicionarPecaRequest> pecas

) {
    public OrdemServicoCreateRequest(Long clienteId, Long veiculoId, String observacoes) {
        this(clienteId, veiculoId, observacoes, List.of(), List.of());
    }
}
