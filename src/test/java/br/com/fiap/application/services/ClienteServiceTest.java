package br.com.fiap.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceInUseException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepositoryPort repositoryPort;

    @Mock
    private VeiculoRepositoryPort veiculoRepositoryPort;

    @Mock
    private OrdemServicoRepositoryPort osRepositoryPort;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("Deve cadastrar um cliente com sucesso")
    void deveCadastrarCliente() {
        Cliente cliente = new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999");
        Cliente clienteSalvo = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");

        when(repositoryPort.buscarPorDocumento("12345678909")).thenReturn(Optional.empty());
        when(repositoryPort.salvar(any(Cliente.class))).thenReturn(clienteSalvo);

        Cliente resultado = clienteService.cadastrarCliente(cliente);

        assertNotNull(resultado.getId());
        assertEquals("12345678909", resultado.getDocumento());
        verify(repositoryPort, times(1)).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao cadastrar com documento já existente")
    void deveLancarResourceAlreadyExistsExceptionQuandoDocumentoExiste() {
        Cliente cliente = new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999");
        when(repositoryPort.buscarPorDocumento("12345678909")).thenReturn(Optional.of(cliente));

        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> clienteService.cadastrarCliente(cliente));

        assertEquals("Cliente com CPF/CNPJ informado já existe.", exception.getMessage());
        verify(repositoryPort, never()).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve listar todos os clientes cadastrados")
    void deveListarTodosOsClientes() {
        Cliente cliente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
        when(repositoryPort.buscarTodos()).thenReturn(List.of(cliente));

        List<Cliente> resultado = clienteService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("Robert", resultado.get(0).getNome());
    }

    @Test
    @DisplayName("Deve buscar cliente por documento com sucesso")
    void deveBuscarPorDocumento() {
        String docBusca = "12345678909";
        Cliente cliente = new Cliente(1L, "Robert", docBusca, "robert@email.com", "11999999999");

        when(repositoryPort.buscarPorDocumento(docBusca)).thenReturn(Optional.of(cliente));

        Cliente resultado = clienteService.buscarPorDocumento(docBusca);

        assertNotNull(resultado);
        assertEquals(docBusca, resultado.getDocumento());
        assertEquals("Robert", resultado.getNome());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar por documento inexistente")
    void deveLancarResourceNotFoundExceptionAoBuscarPorDocumentoInexistente() {
        String docBusca = "12345678909";
        when(repositoryPort.buscarPorDocumento(docBusca)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.buscarPorDocumento(docBusca));

        assertEquals("Cliente não encontrado", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao buscar por documento nulo ou vazio")
    void deveLancarIllegalArgumentExceptionAoBuscarPorDocumentoVazio() {
        assertThrows(IllegalArgumentException.class, () -> clienteService.buscarPorDocumento(" "));
        assertThrows(IllegalArgumentException.class, () -> clienteService.buscarPorDocumento(null));
    }

    @Test
    @DisplayName("Deve atualizar um cliente com sucesso")
    void deveAtualizarCliente() {
        Cliente clienteExistente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
        Cliente dadosAtualizados = new Cliente(1L, "Robert Silva", "12345678909", "robert.silva@email.com", "11988888888");

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(clienteExistente));
        when(repositoryPort.buscarPorDocumento("12345678909")).thenReturn(Optional.of(clienteExistente));
        when(repositoryPort.salvar(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente resultado = clienteService.atualizarCliente(dadosAtualizados);

        assertEquals("Robert Silva", resultado.getNome());
        assertEquals("robert.silva@email.com", resultado.getEmail());
        verify(repositoryPort, times(1)).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao atualizar cliente inexistente")
    void deveLancarResourceNotFoundExceptionAoAtualizarClienteInexistente() {
        Cliente dadosAtualizados = new Cliente(1L, "Robert Silva", "12345678909", "robert.silva@email.com", "11988888888");
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clienteService.atualizarCliente(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao atualizar para um documento de outro cliente")
    void deveLancarResourceAlreadyExistsExceptionAoAtualizarComDocumentoDeOutroCliente() {
        Cliente clienteExistente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
        Cliente outroCliente = new Cliente(2L, "Maria", "98765432100", "maria@email.com", "11977777777");
        Cliente dadosAtualizados = new Cliente(1L, "Robert Silva", "98765432100", "robert.silva@email.com", "11988888888");

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(clienteExistente));
        when(repositoryPort.buscarPorDocumento("98765432100")).thenReturn(Optional.of(outroCliente));

        assertThrows(ResourceAlreadyExistsException.class, () -> clienteService.atualizarCliente(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao atualizar sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoAtualizarSemId() {
        Cliente semId = new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999");

        assertThrows(IllegalArgumentException.class, () -> clienteService.atualizarCliente(semId));
        verify(repositoryPort, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("Deve excluir um cliente com sucesso")
    void deveExcluirCliente() {
        Cliente clienteExistente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(clienteExistente));

        clienteService.excluirCliente(1L);

        verify(repositoryPort, times(1)).excluirPorId(1L);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao excluir cliente inexistente")
    void deveLancarResourceNotFoundExceptionAoExcluirClienteInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clienteService.excluirCliente(1L));
        verify(repositoryPort, never()).excluirPorId(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao excluir sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoExcluirSemId() {
        assertThrows(IllegalArgumentException.class, () -> clienteService.excluirCliente(null));
        verify(repositoryPort, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceInUseException ao excluir cliente com veículos vinculados")
    void deveLancarResourceInUseExceptionAoExcluirClienteComVeiculosVinculados() {
        Cliente clienteExistente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
        Veiculo veiculo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(clienteExistente));
        when(veiculoRepositoryPort.buscarPorClienteId(1L)).thenReturn(List.of(veiculo));

        assertThrows(ResourceInUseException.class, () -> clienteService.excluirCliente(1L));
        verify(repositoryPort, never()).excluirPorId(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceInUseException ao excluir cliente com OS vinculadas")
    void deveLancarResourceInUseExceptionAoExcluirClienteComOSVinculadas() {
        Cliente clienteExistente = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
        OrdemServico os = new OrdemServico(1L, 1L, 1L, null, null, null, null, "obs", null, null, null, null, null, null, null, null);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(clienteExistente));
        when(osRepositoryPort.buscarPorClienteId(1L)).thenReturn(List.of(os));

        assertThrows(ResourceInUseException.class, () -> clienteService.excluirCliente(1L));
        verify(repositoryPort, never()).excluirPorId(any());
    }
}
