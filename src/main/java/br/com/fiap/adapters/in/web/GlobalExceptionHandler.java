package br.com.fiap.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Response<Void>> handleIllegalArgument(
	        IllegalArgumentException ex,
	        WebRequest request) {

	    Response<Void> resposta = new Response<>(
	            "error",
	            ex.getMessage(),
	            null
	    );

	    return ResponseEntity.badRequest().body(resposta);
	}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .findFirst()
                .orElse("Dados inválidos");

        Response<Void> resposta = new Response<>(
                "error",
                mensagem,
                null
        );

        return ResponseEntity.badRequest().body(resposta);
    }

    public ResponseEntity<Response<Void>> handleResourceAlreadyExists(
            ResourceAlreadyExistsException ex,
            WebRequest request) {

        Response<Void> resposta = new Response<>(
                "error",
                ex.getMessage(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(resposta);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Response<Void>> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {

        Response<Void> resposta = new Response<>(
                "error",
                ex.getMessage(),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(resposta);
    }
    
    
}
