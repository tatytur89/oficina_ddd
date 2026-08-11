package br.com.fiap.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentoTest {

    @Test
    @DisplayName("Deve falhar quando o documento for nulo ou vazio")
    void deveFalharQuandoDocumentoNuloOuVazio() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Documento(""));

        assertEquals("Número do documento não pode ser nulo ou vazio.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar quando o CPF for inválido")
    void deveFalharQuandoCpfInvalido() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Documento("11111111111"));

        assertEquals("Número do CPF inválido.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve falhar quando o CNPJ for inválido")
    void deveFalharQuandoCnpjInvalido() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Documento("11111111111111"));

        assertEquals("Número do CNPJ inválido.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve criar documento válido para CPF e CNPJ")
    void deveCriarDocumentoValidoParaCpfECnpj() {
        Documento cpf = new Documento("52998224725");
        assertEquals("52998224725", cpf.getNumero());
        assertEquals(TipoDocumento.CPF, cpf.getTipo());

        Documento cnpj = new Documento("11444777000161");
        assertEquals("11444777000161", cnpj.getNumero());
        assertEquals(TipoDocumento.CNPJ, cnpj.getTipo());
    }
}
