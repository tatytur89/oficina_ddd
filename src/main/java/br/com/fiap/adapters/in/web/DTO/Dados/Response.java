package br.com.fiap.adapters.in.web.DTO.Dados;

public record Response<T>(
		
		
	String status,
	String message,
	T dados
		
		
		
) {}
