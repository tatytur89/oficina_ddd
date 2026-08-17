package br.com.fiap.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class PrecoTest {

    @Test
    void deveCriarPrecoComSucesso() {
        Preco preco = new Preco(150.00);
        assertEquals(0, new BigDecimal("150.00").compareTo(preco.getValor()));
    }

    @Test
    void deveCriarPrecoComBigDecimal() {
        Preco preco = new Preco(new BigDecimal("250.50"));
        assertEquals(new BigDecimal("250.50"), preco.getValor());
    }

    @Test
    void deveAceitarPrecoZero() {
        Preco preco = new Preco(0.0);
        assertEquals(0, BigDecimal.ZERO.compareTo(preco.getValor()));
    }

    @Test
    void deveLancarExcecaoParaPrecoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new Preco(-10.00));
    }

    @Test
    void deveLancarExcecaoParaPrecoNulo() {
        assertThrows(IllegalArgumentException.class, () -> new Preco((BigDecimal) null));
    }

    @Test
    void deveSomarPrecos() {
        Preco preco1 = new Preco(100.00);
        Preco preco2 = new Preco(50.00);
        
        Preco resultado = preco1.somar(preco2);
        
        assertEquals(0, new BigDecimal("150.00").compareTo(resultado.getValor()));
    }

    @Test
    void deveMultiplicarPreco() {
        Preco preco = new Preco(50.00);
        
        Preco resultado = preco.multiplicar(3);
        
        assertEquals(0, new BigDecimal("150.00").compareTo(resultado.getValor()));
    }

    @Test
    void deveVerificarIgualdade() {
        Preco preco1 = new Preco(100.00);
        Preco preco2 = new Preco(100.00);
        
        assertEquals(preco1, preco2);
    }

    @Test
    void deveVerificarDesigualdade() {
        Preco preco1 = new Preco(100.00);
        Preco preco2 = new Preco(200.00);
        
        assertNotEquals(preco1, preco2);
    }
}
