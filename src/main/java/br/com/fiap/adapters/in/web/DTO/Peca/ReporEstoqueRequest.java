package br.com.fiap.adapters.in.web.DTO.Peca;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Quantidade a ser adicionada ao estoque da peça")
public record ReporEstoqueRequest(

    @Schema(example = "10")
    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade deve ser maior que zero")
    Integer quantidade

) {}
