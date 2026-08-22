package br.com.fiap.adapters.in.web.DTO.Peca;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = """
    Dados para atualização de uma peça existente. \
    Não inclui quantidadeEstoque de propósito: a quantidade em estoque só pode ser alterada \
    pelos endpoints dedicados de reposição/baixa de estoque, para não haver dois caminhos \
    diferentes mexendo no mesmo valor.""")
public record PecaUpdateRequest(

    @Schema(example = "Filtro de Óleo")
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    String nome,

    @Schema(example = "Filtro de óleo para motor 1.0 - Marca Premium")
    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    String descricao,

    @Schema(example = "FIL001")
    @NotBlank(message = "O código é obrigatório")
    @Size(min = 2, max = 50, message = "O código deve ter entre 2 e 50 caracteres")
    String codigo,

    @Schema(example = "45.90")
    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser maior que zero")
    BigDecimal preco,

    @Schema(example = "10")
    @NotNull(message = "O estoque mínimo é obrigatório")
    @Min(value = 0, message = "O estoque mínimo não pode ser negativo")
    Integer estoqueMinimo

) {}
