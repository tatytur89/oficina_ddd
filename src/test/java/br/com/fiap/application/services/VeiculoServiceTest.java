package br.com.fiap.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceTest {

    @Mock
    private VeiculoRepositoryPort repositoryPort;

    @Mock
    private ClienteRepositoryPort clienteRepositoryPort;

    @InjectMocks
    private VeiculoService veiculoService;

    private static final Cliente CLIENTE = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");

    @Test
    @DisplayName("Deve cadastrar um veículo com sucesso")
    void deveCadastrarVeiculo() {
        Veiculo veiculo = new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        Veiculo veiculoSalvo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);

        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(repositoryPort.buscarPorPlaca("ABC1D23")).thenReturn(Optional.empty());
        when(repositoryPort.salvar(any(Veiculo.class))).thenReturn(veiculoSalvo);

        Veiculo resultado = veiculoService.cadastrarVeiculo(veiculo);

        assertNotNull(resultado.getId());
        assertEquals("ABC1D23", resultado.getPlaca());
        verify(repositoryPort, times(1)).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao cadastrar veículo com cliente inexistente")
    void deveLancarResourceNotFoundExceptionAoCadastrarComClienteInexistente() {
        Veiculo veiculo = new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> veiculoService.cadastrarVeiculo(veiculo));
        verify(repositoryPort, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao cadastrar com placa já existente")
    void deveLancarResourceAlreadyExistsExceptionQuandoPlacaExiste() {
        Veiculo veiculo = new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(repositoryPort.buscarPorPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));

        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> veiculoService.cadastrarVeiculo(veiculo));

        assertEquals("Veículo com placa informada já existe.", exception.getMessage());
        verify(repositoryPort, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve listar todos os veículos cadastrados")
    void deveListarTodosOsVeiculos() {
        Veiculo veiculo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        when(repositoryPort.buscarTodos()).thenReturn(List.of(veiculo));

        List<Veiculo> resultado = veiculoService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("Corolla", resultado.get(0).getModelo());
    }

    @Test
    @DisplayName("Deve listar veículos por cliente")
    void deveListarPorCliente() {
        Veiculo veiculo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        when(repositoryPort.buscarPorClienteId(1L)).thenReturn(List.of(veiculo));

        List<Veiculo> resultado = veiculoService.listarPorCliente(1L);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve buscar veículo por placa com sucesso")
    void deveBuscarPorPlaca() {
        Veiculo veiculo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        when(repositoryPort.buscarPorPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));

        Veiculo resultado = veiculoService.buscarPorPlaca("abc-1d23");

        assertNotNull(resultado);
        assertEquals("Corolla", resultado.getModelo());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar por placa inexistente")
    void deveLancarResourceNotFoundExceptionAoBuscarPorPlacaInexistente() {
        when(repositoryPort.buscarPorPlaca("ABC1D23")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> veiculoService.buscarPorPlaca("ABC1D23"));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao buscar por placa nula ou vazia")
    void deveLancarIllegalArgumentExceptionAoBuscarPorPlacaVazia() {
        assertThrows(IllegalArgumentException.class, () -> veiculoService.buscarPorPlaca(" "));
        assertThrows(IllegalArgumentException.class, () -> veiculoService.buscarPorPlaca(null));
    }

    @Test
    @DisplayName("Deve atualizar um veículo com sucesso")
    void deveAtualizarVeiculo() {
        Veiculo veiculoExistente = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        Veiculo dadosAtualizados = new Veiculo(1L, "Toyota", "Corolla Cross", 2023, "ABC1D23", 1L);

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(veiculoExistente));
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(repositoryPort.buscarPorPlaca("ABC1D23")).thenReturn(Optional.of(veiculoExistente));
        when(repositoryPort.salvar(any(Veiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Veiculo resultado = veiculoService.atualizarVeiculo(dadosAtualizados);

        assertEquals("Corolla Cross", resultado.getModelo());
        assertEquals(2023, resultado.getAno());
        verify(repositoryPort, times(1)).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao atualizar veículo inexistente")
    void deveLancarResourceNotFoundExceptionAoAtualizarVeiculoInexistente() {
        Veiculo dadosAtualizados = new Veiculo(1L, "Toyota", "Corolla Cross", 2023, "ABC1D23", 1L);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> veiculoService.atualizarVeiculo(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao atualizar veículo com cliente inexistente")
    void deveLancarResourceNotFoundExceptionAoAtualizarComClienteInexistente() {
        Veiculo veiculoExistente = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        Veiculo dadosAtualizados = new Veiculo(1L, "Toyota", "Corolla Cross", 2023, "ABC1D23", 1L);

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(veiculoExistente));
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> veiculoService.atualizarVeiculo(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao atualizar para a placa de outro veículo")
    void deveLancarResourceAlreadyExistsExceptionAoAtualizarComPlacaDeOutroVeiculo() {
        Veiculo veiculoExistente = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        Veiculo outroVeiculo = new Veiculo(2L, "Honda", "Civic", 2021, "XYZ9K88", 1L);
        Veiculo dadosAtualizados = new Veiculo(1L, "Toyota", "Corolla", 2022, "XYZ9K88", 1L);

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(veiculoExistente));
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(repositoryPort.buscarPorPlaca("XYZ9K88")).thenReturn(Optional.of(outroVeiculo));

        assertThrows(ResourceAlreadyExistsException.class, () -> veiculoService.atualizarVeiculo(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao atualizar sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoAtualizarSemId() {
        Veiculo semId = new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", 1L);

        assertThrows(IllegalArgumentException.class, () -> veiculoService.atualizarVeiculo(semId));
        verify(repositoryPort, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("Deve excluir um veículo com sucesso")
    void deveExcluirVeiculo() {
        Veiculo veiculoExistente = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(veiculoExistente));

        veiculoService.excluirVeiculo(1L);

        verify(repositoryPort, times(1)).excluirPorId(1L);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao excluir veículo inexistente")
    void deveLancarResourceNotFoundExceptionAoExcluirVeiculoInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> veiculoService.excluirVeiculo(1L));
        verify(repositoryPort, never()).excluirPorId(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao excluir sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoExcluirSemId() {
        assertThrows(IllegalArgumentException.class, () -> veiculoService.excluirVeiculo(null));
        verify(repositoryPort, never()).buscarPorId(any());
    }
}
