package br.com.fiap.adapters.in.web.DTO.Servico;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualização de um serviço existente")
public record ServicoUpdateRequest(

    @Schema(example = "Troca de Óleo")
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    String nome,

    @Schema(example = "Troca de óleo do motor 1.0 com filtro de óleo")
    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    String descricao,

    @Schema(example = "150.00")
    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser maior que zero")
    BigDecimal preco,

    @Schema(
        example = "MANUTENCAO",
        allowableValues = {"REVISAO", "MANUTENCAO", "TROCA_PECA", "ALINHAMENTO", "BALANCEAMENTO", "MECANICA_GERAL", "ELETRICA", "SUSPENSAO", "FREIOS"}
    )
    @NotBlank(message = "O tipo é obrigatório")
    @Pattern(
        regexp = "REVISAO|MANUTENCAO|TROCA_PECA|ALINHAMENTO|BALANCEAMENTO|MECANICA_GERAL|ELETRICA|SUSPENSAO|FREIOS",
        message = "O tipo deve ser um dos valores: REVISAO, MANUTENCAO, TROCA_PECA, ALINHAMENTO, BALANCEAMENTO, MECANICA_GERAL, ELETRICA, SUSPENSAO, FREIOS"
    )
    String tipo,

    @Schema(example = "60")
    @NotNull(message = "O tempo estimado é obrigatório")
    @Min(value = 1, message = "O tempo estimado deve ser maior que 0")
    Integer tempoEstimadoMinutos

) {}
