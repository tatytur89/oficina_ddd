package br.com.fiap.adapters.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.domain.entities.Veiculo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(VeiculoPersistenceAdapter.class)
class VeiculoPersistenceAdapterTest {

    @Autowired
    private VeiculoPersistenceAdapter veiculoPersistenceAdapter;

    @Autowired
    private VeiculoJpaRepository veiculoJpaRepository;

    @Test
    @DisplayName("Deve salvar um veículo e retorná-lo com ID gerado")
    void deveSalvarVeiculo() {
        Veiculo veiculo = new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L);

        Veiculo salvo = veiculoPersistenceAdapter.salvar(veiculo);

        assertNotNull(salvo.getId());
        assertEquals("Corolla", salvo.getModelo());
        assertEquals("ABC1D23", salvo.getPlaca());
    }

    @Test
    @DisplayName("Deve buscar todos os veículos cadastrados")
    void deveBuscarTodos() {
        veiculoPersistenceAdapter.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L));
        veiculoPersistenceAdapter.salvar(new Veiculo(null, "Honda", "Civic", 2021, "XYZ9K88", 2L));

        List<Veiculo> veiculos = veiculoPersistenceAdapter.buscarTodos();

        assertEquals(2, veiculos.size());
    }

    @Test
    @DisplayName("Deve buscar veículos por cliente")
    void deveBuscarPorClienteId() {
        veiculoPersistenceAdapter.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L));
        veiculoPersistenceAdapter.salvar(new Veiculo(null, "Honda", "Civic", 2021, "XYZ9K88", 2L));

        List<Veiculo> veiculos = veiculoPersistenceAdapter.buscarPorClienteId(1L);

        assertEquals(1, veiculos.size());
        assertEquals("Corolla", veiculos.get(0).getModelo());
    }

    @Test
    @DisplayName("Deve buscar veículo por placa")
    void deveBuscarPorPlaca() {
        veiculoPersistenceAdapter.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L));

        Optional<Veiculo> resultado = veiculoPersistenceAdapter.buscarPorPlaca("ABC1D23");

        assertTrue(resultado.isPresent());
        assertEquals("Corolla", resultado.get().getModelo());
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar placa inexistente")
    void deveRetornarVazioAoBuscarPlacaInexistente() {
        Optional<Veiculo> resultado = veiculoPersistenceAdapter.buscarPorPlaca("ABC1D23");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve buscar veículo por ID")
    void deveBuscarPorId() {
        Veiculo salvo = veiculoPersistenceAdapter.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L));

        Optional<Veiculo> resultado = veiculoPersistenceAdapter.buscarPorId(salvo.getId());

        assertTrue(resultado.isPresent());
        assertEquals("Corolla", resultado.get().getModelo());
    }

    @Test
    @DisplayName("Deve excluir veículo por ID")
    void deveExcluirPorId() {
        Veiculo salvo = veiculoPersistenceAdapter.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L));

        veiculoPersistenceAdapter.excluirPorId(salvo.getId());

        assertTrue(veiculoPersistenceAdapter.buscarPorId(salvo.getId()).isEmpty());
    }

    @Test
    @DisplayName("Deve ignorar registros com placa inválida ao listar todos, sem quebrar a listagem")
    void deveIgnorarVeiculoComPlacaInvalidaNaListagem() {
        veiculoPersistenceAdapter.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L));
        veiculoJpaRepository.save(new VeiculoJpaEntity(null, "Fiat", "Uno", 2000, "INVALIDA", 1L));

        List<Veiculo> veiculos = veiculoPersistenceAdapter.buscarTodos();

        assertEquals(1, veiculos.size());
        assertEquals("Corolla", veiculos.get(0).getModelo());
    }

    @Test
    @DisplayName("Deve ignorar registros com placa inválida ao listar por cliente, sem quebrar a listagem")
    void deveIgnorarVeiculoComPlacaInvalidaAoListarPorCliente() {
        veiculoPersistenceAdapter.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L));
        veiculoJpaRepository.save(new VeiculoJpaEntity(null, "Fiat", "Uno", 2000, "INVALIDA", 1L));

        List<Veiculo> veiculos = veiculoPersistenceAdapter.buscarPorClienteId(1L);

        assertEquals(1, veiculos.size());
        assertEquals("Corolla", veiculos.get(0).getModelo());
    }
}
