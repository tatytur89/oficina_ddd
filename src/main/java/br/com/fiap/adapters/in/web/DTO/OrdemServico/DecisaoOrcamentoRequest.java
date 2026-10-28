package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Notificação externa da decisão sobre uma versão do orçamento")
public record DecisaoOrcamentoRequest(
        @NotBlank String chave,
        @NotNull @PositiveOrZero Long versaoOrcamento,
        @NotNull Boolean aprovado) {
}
