package br.com.fiap.adapters.in.web;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastro de uma nova peça")
public class PecaRequestDTO {

    @Schema(
        description = "Nome da peça ou insumo",
        example = "Filtro de Óleo",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Schema(
        description = "Descrição detalhada da peça",
        example = "Filtro de óleo para motor 1.0 - Marca Premium"
    )
    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String descricao;

    @Schema(
        description = "Código interno da peça (deve ser único)",
        example = "FIL001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "O código é obrigatório")
    @Size(min = 2, max = 50, message = "O código deve ter entre 2 e 50 caracteres")
    private String codigo;

    @Schema(
        description = "Preço unitário da peça em reais (R$)",
        example = "45.90",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser maior que zero")
    private BigDecimal preco;

    @Schema(
        description = "Quantidade atual em estoque",
        example = "50",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 0, message = "A quantidade não pode ser negativa")
    private Integer quantidadeEstoque;

    @Schema(
        description = "Estoque mínimo para gerar alerta de reposição",
        example = "10",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O estoque mínimo é obrigatório")
    @Min(value = 0, message = "O estoque mínimo não pode ser negativo")
    private Integer estoqueMinimo;

    public PecaRequestDTO() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public Integer getQuantidadeEstoque() { return quantidadeEstoque; }
    public void setQuantidadeEstoque(Integer quantidadeEstoque) { this.quantidadeEstoque = quantidadeEstoque; }

    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
}
