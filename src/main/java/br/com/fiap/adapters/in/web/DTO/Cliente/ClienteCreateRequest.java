package br.com.fiap.adapters.in.web.DTO.Cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClienteCreateRequest(
		
	@NotBlank String nome,
    @NotBlank
    @Pattern(regexp = "\\d{11}|\\d{14}")
    String documento,
    @Email String email,
    @Pattern(regexp = "\\d{10,11}")
    String telefone
		
) {}
