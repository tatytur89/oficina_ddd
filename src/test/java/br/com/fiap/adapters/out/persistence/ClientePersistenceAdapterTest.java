package br.com.fiap.adapters.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.domain.entities.Cliente;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ClientePersistenceAdapter.class)
class ClientePersistenceAdapterTest {

    @Autowired
    private ClientePersistenceAdapter clientePersistenceAdapter;

    @Autowired
    private ClienteJpaRepository clienteJpaRepository;

    @Test
    @DisplayName("Deve salvar um cliente e retorná-lo com ID gerado")
    void deveSalvarCliente() {
        Cliente cliente = new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999");

        Cliente salvo = clientePersistenceAdapter.salvar(cliente);

        assertNotNull(salvo.getId());
        assertEquals("Robert", salvo.getNome());
        assertEquals("12345678909", salvo.getDocumento());
    }

    @Test
    @DisplayName("Deve buscar todos os clientes cadastrados")
    void deveBuscarTodos() {
        clientePersistenceAdapter.salvar(new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999"));
        clientePersistenceAdapter.salvar(new Cliente(null, "Maria", "98765432100", "maria@email.com", "11977777777"));

        List<Cliente> clientes = clientePersistenceAdapter.buscarTodos();

        assertEquals(2, clientes.size());
    }

    @Test
    @DisplayName("Deve buscar cliente por documento")
    void deveBuscarPorDocumento() {
        clientePersistenceAdapter.salvar(new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999"));

        Optional<Cliente> resultado = clientePersistenceAdapter.buscarPorDocumento("12345678909");

        assertTrue(resultado.isPresent());
        assertEquals("Robert", resultado.get().getNome());
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar documento inexistente")
    void deveRetornarVazioAoBuscarDocumentoInexistente() {
        Optional<Cliente> resultado = clientePersistenceAdapter.buscarPorDocumento("12345678909");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve buscar cliente por ID")
    void deveBuscarPorId() {
        Cliente salvo = clientePersistenceAdapter.salvar(new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999"));

        Optional<Cliente> resultado = clientePersistenceAdapter.buscarPorId(salvo.getId());

        assertTrue(resultado.isPresent());
        assertEquals("Robert", resultado.get().getNome());
    }

    @Test
    @DisplayName("Deve excluir cliente por ID")
    void deveExcluirPorId() {
        Cliente salvo = clientePersistenceAdapter.salvar(new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999"));

        clientePersistenceAdapter.excluirPorId(salvo.getId());

        assertTrue(clientePersistenceAdapter.buscarPorId(salvo.getId()).isEmpty());
    }

    @Test
    @DisplayName("Deve ignorar registros com documento inválido ao listar todos, sem quebrar a listagem")
    void deveIgnorarClienteComDocumentoInvalidoNaListagem() {
        clientePersistenceAdapter.salvar(new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999"));
        clienteJpaRepository.save(new ClienteJpaEntity(null, "Corrompido", "123", null, null));

        List<Cliente> clientes = clientePersistenceAdapter.buscarTodos();

        assertEquals(1, clientes.size());
        assertEquals("Robert", clientes.get(0).getNome());
    }
}
