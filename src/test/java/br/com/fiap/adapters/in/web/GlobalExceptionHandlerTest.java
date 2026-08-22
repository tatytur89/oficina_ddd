package br.com.fiap.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.application.exceptions.InvalidCredentialsException;
import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceInUseException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private MethodArgumentNotValidException validationException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void deveTratarIllegalArgumentExceptionComo400() {
        ResponseEntity<Response<Void>> response = handler.handleIllegalArgument(new IllegalArgumentException("mensagem"), null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("error", response.getBody().status());
        assertEquals("mensagem", response.getBody().message());
    }

    @Test
    void deveTratarIllegalStateExceptionComo400() {
        ResponseEntity<Response<Void>> response = handler.handleIllegalState(new IllegalStateException("transição inválida"), null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("transição inválida", response.getBody().message());
    }

    @Test
    void deveTratarValidationExceptionExtraindoPrimeiroErro() {
        FieldError fieldError = new FieldError("objeto", "nome", "O nome é obrigatório");
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Response<Void>> response = handler.handleValidation(validationException, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("nome: O nome é obrigatório", response.getBody().message());
    }

    @Test
    void deveTratarValidationExceptionSemErrosComMensagemPadrao() {
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Response<Void>> response = handler.handleValidation(validationException, null);

        assertEquals("Dados inválidos", response.getBody().message());
    }

    @Test
    void deveTratarResourceAlreadyExistsExceptionComo409() {
        ResponseEntity<Response<Void>> response = handler.handleResourceAlreadyExists(
                new ResourceAlreadyExistsException("já existe"), null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("já existe", response.getBody().message());
    }

    @Test
    void deveTratarResourceNotFoundExceptionComo404() {
        ResponseEntity<Response<Void>> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("não encontrado"), null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("não encontrado", response.getBody().message());
    }

    @Test
    void deveTratarResourceInUseExceptionComo409() {
        ResponseEntity<Response<Void>> response = handler.handleResourceInUse(
                new ResourceInUseException("em uso"), null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("em uso", response.getBody().message());
    }

    @Test
    void deveTratarInvalidCredentialsExceptionComo401() {
        ResponseEntity<Response<Void>> response = handler.handleInvalidCredentials(
                new InvalidCredentialsException("Credenciais inválidas"), null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciais inválidas", response.getBody().message());
    }

    @Test
    void deveTratarDataIntegrityViolationExceptionComo409() {
        ResponseEntity<Response<Void>> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("violação de constraint"), null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("O registro informado já existe ou viola uma restrição do banco de dados.", response.getBody().message());
    }
}
