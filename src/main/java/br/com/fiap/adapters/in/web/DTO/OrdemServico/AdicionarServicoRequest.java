package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Dados para adicionar um serviço a uma OS em diagnóstico")
public record AdicionarServicoRequest(

    @Schema(example = "1")
    @NotNull(message = "O ID do serviço é obrigatório")
    @Positive
    Long servicoId,

    @Schema(example = "1")
    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade deve ser maior que zero")
    Integer quantidade

) {}
