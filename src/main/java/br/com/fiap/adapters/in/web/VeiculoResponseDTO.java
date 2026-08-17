package br.com.fiap.adapters.in.web;

import br.com.fiap.domain.entities.Veiculo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um veículo")
public class VeiculoResponseDTO {

    @Schema(
        description = "Identificador único do veículo",
        example = "1"
    )
    private Long id;

    @Schema(
        description = "Marca/fabricante do veículo",
        example = "Toyota"
    )
    private String marca;

    @Schema(
        description = "Modelo do veículo",
        example = "Corolla"
    )
    private String modelo;

    @Schema(
        description = "Ano de fabricação",
        example = "2022"
    )
    private Integer ano;

    @Schema(
        description = "Placa do veículo (formato Mercosul ou antigo)",
        example = "ABC1D23"
    )
    private String placa;

    @Schema(
        description = "ID do cliente proprietário",
        example = "1"
    )
    private Long clienteId;

    public VeiculoResponseDTO(Veiculo veiculo) {
        this.id = veiculo.getId();
        this.marca = veiculo.getMarca();
        this.modelo = veiculo.getModelo();
        this.ano = veiculo.getAno();
        this.placa = veiculo.getPlaca();
        this.clienteId = veiculo.getClienteId();
    }

    public Long getId() { return id; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public Integer getAno() { return ano; }
    public String getPlaca() { return placa; }
    public Long getClienteId() { return clienteId; }
}
