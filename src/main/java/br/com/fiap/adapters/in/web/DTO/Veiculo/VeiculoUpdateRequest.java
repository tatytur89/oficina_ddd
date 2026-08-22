package br.com.fiap.adapters.in.web.DTO.Veiculo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para atualização de um veículo existente")
public record VeiculoUpdateRequest(

    @Schema(example = "Toyota")
    @NotBlank(message = "A marca é obrigatória")
    @Size(min = 2, max = 50, message = "A marca deve ter entre 2 e 50 caracteres")
    String marca,

    @Schema(example = "Corolla")
    @NotBlank(message = "O modelo é obrigatório")
    @Size(min = 2, max = 100, message = "O modelo deve ter entre 2 e 100 caracteres")
    String modelo,

    @Schema(example = "2022", minimum = "1900", maximum = "2030")
    @NotNull(message = "O ano é obrigatório")
    @Min(value = 1900, message = "O ano deve ser maior que 1900")
    @Max(value = 2030, message = "O ano deve ser menor ou igual a 2030")
    Integer ano,

    @Schema(example = "ABC1D23", pattern = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}")
    @NotBlank(message = "A placa é obrigatória")
    @Pattern(regexp = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}", message = "Formato de placa inválido. Use ABC1234 ou ABC1D23")
    String placa,

    @Schema(example = "1")
    @NotNull(message = "O ID do cliente é obrigatório")
    Long clienteId

) {}
