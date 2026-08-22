package br.com.fiap.adapters.in.web.DTO.Cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para cadastro de um novo cliente")
public record ClienteCreateRequest(

	@Schema(example = "João da Silva")
	@NotBlank(message = "O nome é obrigatório")
	@Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
	String nome,

    @Schema(example = "12345678909")
    @NotBlank(message = "O documento é obrigatório")
    @Pattern(regexp = "\\d{11}|\\d{14}", message = "O documento deve conter 11 dígitos (CPF) ou 14 dígitos (CNPJ)")
    String documento,

    @Schema(example = "joao.silva@email.com")
    @Email(message = "Formato de e-mail inválido")
    String email,

    @Schema(example = "11999998888")
    @Pattern(regexp = "\\d{10,11}", message = "O telefone deve conter 10 ou 11 dígitos")
    String telefone

) {}
