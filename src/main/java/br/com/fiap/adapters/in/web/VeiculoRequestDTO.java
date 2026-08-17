package br.com.fiap.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastro de um novo veículo")
public class VeiculoRequestDTO {

    @Schema(
        description = "Marca/fabricante do veículo",
        example = "Toyota",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "A marca é obrigatória")
    @Size(min = 2, max = 50, message = "A marca deve ter entre 2 e 50 caracteres")
    private String marca;

    @Schema(
        description = "Modelo do veículo",
        example = "Corolla",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "O modelo é obrigatório")
    @Size(min = 2, max = 100, message = "O modelo deve ter entre 2 e 100 caracteres")
    private String modelo;

    @Schema(
        description = "Ano de fabricação do veículo",
        example = "2022",
        minimum = "1900",
        maximum = "2030",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O ano é obrigatório")
    @Min(value = 1900, message = "O ano deve ser maior que 1900")
    @Max(value = 2030, message = "O ano deve ser menor ou igual a 2030")
    private Integer ano;

    @Schema(
        description = "Placa do veículo. Formatos aceitos: ABC1234 (antigo) ou ABC1D23 (Mercosul)",
        example = "ABC1D23",
        pattern = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "A placa é obrigatória")
    @Pattern(regexp = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}", message = "Formato de placa inválido. Use ABC1234 ou ABC1D23")
    private String placa;

    @Schema(
        description = "ID do cliente proprietário do veículo (deve existir)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "O ID do cliente é obrigatório")
    private Long clienteId;

    public VeiculoRequestDTO() {}

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
}
