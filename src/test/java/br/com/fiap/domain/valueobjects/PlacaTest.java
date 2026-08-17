package br.com.fiap.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlacaTest {

    @Test
    void deveCriarPlacaValidaFormatoAntigo() {
        Placa placa = new Placa("ABC1234");
        assertEquals("ABC1234", placa.getValor());
    }

    @Test
    void deveCriarPlacaValidaFormatoMercosul() {
        Placa placa = new Placa("ABC1D23");
        assertEquals("ABC1D23", placa.getValor());
    }

    @Test
    void deveConverterParaMaiusculo() {
        Placa placa = new Placa("abc1234");
        assertEquals("ABC1234", placa.getValor());
    }

    @Test
    void deveRemoverCaracteresEspeciais() {
        Placa placa = new Placa("ABC-1234");
        assertEquals("ABC1234", placa.getValor());
    }

    @Test
    void deveLancarExcecaoParaPlacaNula() {
        assertThrows(IllegalArgumentException.class, () -> new Placa(null));
    }

    @Test
    void deveLancarExcecaoParaPlacaVazia() {
        assertThrows(IllegalArgumentException.class, () -> new Placa(""));
    }

    @Test
    void deveLancarExcecaoParaPlacaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new Placa("1234567"));
    }

    @Test
    void deveLancarExcecaoParaPlacaComMenosDigitos() {
        assertThrows(IllegalArgumentException.class, () -> new Placa("ABC123"));
    }

    @Test
    void deveVerificarIgualdade() {
        Placa placa1 = new Placa("ABC1234");
        Placa placa2 = new Placa("ABC1234");
        assertEquals(placa1, placa2);
    }

    @Test
    void deveVerificarDesigualdade() {
        Placa placa1 = new Placa("ABC1234");
        Placa placa2 = new Placa("DEF5678");
        assertNotEquals(placa1, placa2);
    }
}
