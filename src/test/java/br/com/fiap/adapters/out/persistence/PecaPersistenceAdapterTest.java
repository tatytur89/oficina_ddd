package br.com.fiap.adapters.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.Preco;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(PecaPersistenceAdapter.class)
class PecaPersistenceAdapterTest {

    @Autowired
    private PecaPersistenceAdapter pecaPersistenceAdapter;

    private Peca novaPeca(String codigo, int quantidadeEstoque, int estoqueMinimo) {
        return new Peca(null, "Filtro de Óleo", "Descrição", codigo, new Preco(BigDecimal.valueOf(45.90)), quantidadeEstoque, estoqueMinimo);
    }

    @Test
    @DisplayName("Deve salvar uma peça e retorná-la com ID gerado")
    void deveSalvarPeca() {
        Peca salva = pecaPersistenceAdapter.salvar(novaPeca("FIL001", 50, 10));

        assertNotNull(salva.getId());
        assertEquals("FIL001", salva.getCodigo());
    }

    @Test
    @DisplayName("Deve buscar todas as peças cadastradas")
    void deveBuscarTodas() {
        pecaPersistenceAdapter.salvar(novaPeca("FIL001", 50, 10));
        pecaPersistenceAdapter.salvar(novaPeca("FIL002", 20, 5));

        List<Peca> pecas = pecaPersistenceAdapter.buscarTodas();

        assertEquals(2, pecas.size());
    }

    @Test
    @DisplayName("Deve buscar peça por código")
    void deveBuscarPorCodigo() {
        pecaPersistenceAdapter.salvar(novaPeca("FIL001", 50, 10));

        Optional<Peca> resultado = pecaPersistenceAdapter.buscarPorCodigo("FIL001");

        assertTrue(resultado.isPresent());
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar código inexistente")
    void deveRetornarVazioAoBuscarCodigoInexistente() {
        Optional<Peca> resultado = pecaPersistenceAdapter.buscarPorCodigo("FIL001");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve buscar peça por ID")
    void deveBuscarPorId() {
        Peca salva = pecaPersistenceAdapter.salvar(novaPeca("FIL001", 50, 10));

        Optional<Peca> resultado = pecaPersistenceAdapter.buscarPorId(salva.getId());

        assertTrue(resultado.isPresent());
    }

    @Test
    @DisplayName("Deve buscar peças com estoque baixo")
    void deveBuscarEstoqueBaixo() {
        pecaPersistenceAdapter.salvar(novaPeca("FIL001", 5, 10));
        pecaPersistenceAdapter.salvar(novaPeca("FIL002", 50, 10));

        List<Peca> resultado = pecaPersistenceAdapter.buscarEstoqueBaixo(10);

        assertEquals(1, resultado.size());
        assertEquals("FIL001", resultado.get(0).getCodigo());
    }

    @Test
    @DisplayName("Deve excluir peça por ID")
    void deveExcluirPorId() {
        Peca salva = pecaPersistenceAdapter.salvar(novaPeca("FIL001", 50, 10));

        pecaPersistenceAdapter.excluirPorId(salva.getId());

        assertTrue(pecaPersistenceAdapter.buscarPorId(salva.getId()).isEmpty());
    }
}
