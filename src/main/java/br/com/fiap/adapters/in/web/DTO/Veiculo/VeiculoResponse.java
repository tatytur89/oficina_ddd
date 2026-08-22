package br.com.fiap.adapters.in.web.DTO.Veiculo;

import br.com.fiap.domain.entities.Veiculo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um veículo")
public record VeiculoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Toyota") String marca,
        @Schema(example = "Corolla") String modelo,
        @Schema(example = "2022") Integer ano,
        @Schema(example = "ABC1D23") String placa,
        @Schema(example = "1") Long clienteId
) {
    public VeiculoResponse(Veiculo veiculo) {
        this(
            veiculo.getId(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getAno(),
            veiculo.getPlaca(),
            veiculo.getClienteId()
        );
    }
}
