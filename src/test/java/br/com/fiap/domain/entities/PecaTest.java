package br.com.fiap.domain.entities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import br.com.fiap.domain.valueobjects.Preco;

class PecaTest {

    @Test
    void deveCriarPecaComSucesso() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Filtro para motor 1.0", 
            "FIL001", new Preco(45.90), 50, 10);
        
        assertEquals(1L, peca.getId());
        assertEquals("Filtro de Óleo", peca.getNome());
        assertEquals("Filtro para motor 1.0", peca.getDescricao());
        assertEquals("FIL001", peca.getCodigo());
        assertEquals(50, peca.getQuantidadeEstoque());
        assertEquals(10, peca.getEstoqueMinimo());
        assertFalse(peca.estoqueBaixo());
    }

    @Test
    void deveBaixarEstoqueComSucesso() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Filtro", 
            "FIL001", new Preco(45.90), 50, 10);
        
        peca.baixarEstoque(10);
        
        assertEquals(40, peca.getQuantidadeEstoque());
    }

    @Test
    void deveLancarExcecaoParaEstoqueInsuficiente() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Filtro", 
            "FIL001", new Preco(45.90), 5, 10);
        
        assertThrows(IllegalStateException.class, () -> peca.baixarEstoque(10));
    }

    @Test
    void deveLancarExcecaoParaQuantidadeInvalida() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Filtro", 
            "FIL001", new Preco(45.90), 50, 10);
        
        assertThrows(IllegalArgumentException.class, () -> peca.baixarEstoque(0));
        assertThrows(IllegalArgumentException.class, () -> peca.baixarEstoque(-1));
    }

    @Test
    void deveReporEstoqueComSucesso() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Filtro", 
            "FIL001", new Preco(45.90), 50, 10);
        
        peca.reporEstoque(20);
        
        assertEquals(70, peca.getQuantidadeEstoque());
    }

    @Test
    void deveDetectarEstoqueBaixo() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Filtro", 
            "FIL001", new Preco(45.90), 5, 10);
        
        assertTrue(peca.estoqueBaixo());
    }

    @Test
    void deveDetectarEstoqueNormal() {
        Peca peca = new Peca(1L, "Filtro de Óleo", "Filtro", 
            "FIL001", new Preco(45.90), 50, 10);
        
        assertFalse(peca.estoqueBaixo());
    }
}
