package br.com.fiap.domain.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;

class ServicoTest {

    @Test
    void deveCriarServicoComSucesso() {
        Servico servico = new Servico(1L, "Troca de Óleo", "Troca de óleo do motor", 
            new Preco(150.00), TipoServico.MANUTENCAO, 60);
        
        assertEquals(1L, servico.getId());
        assertEquals("Troca de Óleo", servico.getNome());
        assertEquals("Troca de óleo do motor", servico.getDescricao());
        assertEquals(0, new BigDecimal("150.00").compareTo(servico.getPreco().getValor()));
        assertEquals(TipoServico.MANUTENCAO, servico.getTipo());
        assertEquals(60, servico.getTempoEstimadoMinutos());
    }

    @Test
    void deveCriarServicoComConstrutorAlternativo() {
        Servico servico = new Servico(1L, "Alinhamento", "Alinhamento completo", 
            100.00, "ALINHAMENTO", 45);
        
        assertEquals(0, new BigDecimal("100.00").compareTo(servico.getPreco().getValor()));
        assertEquals(TipoServico.ALINHAMENTO, servico.getTipo());
    }
}
