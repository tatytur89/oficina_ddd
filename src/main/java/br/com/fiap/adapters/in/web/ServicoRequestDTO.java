package br.com.fiap.adapters.in.web;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastro de um novo serviço")
public class ServicoRequestDTO {

    @Schema(
        description = "Nome do serviço de manutenção",
        example = "Troca de Óleo",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Schema(
        description = "Descrição detalhada do serviço",
        example = "Troca de óleo do motor 1.0 com filtro de óleo"
    )
    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String descricao;

    @Schema(
        description = "Preço do serviço em reais (R$)",
        example = "150.00",
        minimum = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser maior que zero")
    private BigDecimal preco;

    @Schema(
        description = "Tipo do serviço de manutenção",
        example = "MANUTENCAO",
        allowableValues = {"REVISAO", "MANUTENCAO", "TROCA_PECA", "ALINHAMENTO", "BALANCEAMENTO", "MECANICA_GERAL", "ELETRICA", "SUSPENSAO", "FREIOS"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "O tipo é obrigatório")
    private String tipo;

    @Schema(
        description = "Tempo estimado de execução em minutos",
        example = "60",
        minimum = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O tempo estimado é obrigatório")
    @Min(value = 1, message = "O tempo estimado deve ser maior que 0")
    private Integer tempoEstimadoMinutos;

    public ServicoRequestDTO() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getTempoEstimadoMinutos() { return tempoEstimadoMinutos; }
    public void setTempoEstimadoMinutos(Integer tempoEstimadoMinutos) { this.tempoEstimadoMinutos = tempoEstimadoMinutos; }
}
