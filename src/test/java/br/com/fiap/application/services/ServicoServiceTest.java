package br.com.fiap.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;
import br.com.fiap.ports.out.ServicoRepositoryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @Mock
    private ServicoRepositoryPort repositoryPort;

    @InjectMocks
    private ServicoService servicoService;

    private Servico novoServico(Long id, String nome) {
        return new Servico(id, nome, "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);
    }

    @Test
    @DisplayName("Deve cadastrar um serviço com sucesso")
    void deveCadastrarServico() {
        Servico servico = novoServico(null, "Troca de Óleo");
        Servico servicoSalvo = novoServico(1L, "Troca de Óleo");

        when(repositoryPort.buscarPorNome("Troca de Óleo")).thenReturn(Optional.empty());
        when(repositoryPort.salvar(any(Servico.class))).thenReturn(servicoSalvo);

        Servico resultado = servicoService.cadastrarServico(servico);

        assertNotNull(resultado.getId());
        assertEquals("Troca de Óleo", resultado.getNome());
        verify(repositoryPort, times(1)).salvar(any(Servico.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao cadastrar com nome já existente")
    void deveLancarResourceAlreadyExistsExceptionQuandoNomeExiste() {
        Servico servico = novoServico(null, "Troca de Óleo");
        when(repositoryPort.buscarPorNome("Troca de Óleo")).thenReturn(Optional.of(servico));

        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> servicoService.cadastrarServico(servico));

        assertEquals("Serviço com nome informado já existe.", exception.getMessage());
        verify(repositoryPort, never()).salvar(any(Servico.class));
    }

    @Test
    @DisplayName("Deve listar todos os serviços cadastrados")
    void deveListarTodosOsServicos() {
        Servico servico = novoServico(1L, "Troca de Óleo");
        when(repositoryPort.buscarTodos()).thenReturn(List.of(servico));

        List<Servico> resultado = servicoService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals("Troca de Óleo", resultado.get(0).getNome());
    }

    @Test
    @DisplayName("Deve buscar serviço por ID com sucesso")
    void deveBuscarPorId() {
        Servico servico = novoServico(1L, "Troca de Óleo");
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(servico));

        Servico resultado = servicoService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("Troca de Óleo", resultado.getNome());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar por ID inexistente")
    void deveLancarResourceNotFoundExceptionAoBuscarPorIdInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicoService.buscarPorId(1L));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao buscar por ID nulo")
    void deveLancarIllegalArgumentExceptionAoBuscarPorIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> servicoService.buscarPorId(null));
    }

    @Test
    @DisplayName("Deve atualizar um serviço com sucesso")
    void deveAtualizarServico() {
        Servico servicoExistente = novoServico(1L, "Troca de Óleo");
        Servico dadosAtualizados = new Servico(1L, "Troca de Óleo Sintético", "Descrição nova",
                new Preco(BigDecimal.valueOf(200.00)), TipoServico.MANUTENCAO, 45);

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(servicoExistente));
        when(repositoryPort.buscarPorNome("Troca de Óleo Sintético")).thenReturn(Optional.of(servicoExistente));
        when(repositoryPort.salvar(any(Servico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Servico resultado = servicoService.atualizarServico(dadosAtualizados);

        assertEquals("Troca de Óleo Sintético", resultado.getNome());
        assertEquals(45, resultado.getTempoEstimadoMinutos());
        verify(repositoryPort, times(1)).salvar(any(Servico.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao atualizar serviço inexistente")
    void deveLancarResourceNotFoundExceptionAoAtualizarServicoInexistente() {
        Servico dadosAtualizados = novoServico(1L, "Troca de Óleo");
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicoService.atualizarServico(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Servico.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao atualizar para o nome de outro serviço")
    void deveLancarResourceAlreadyExistsExceptionAoAtualizarComNomeDeOutroServico() {
        Servico servicoExistente = novoServico(1L, "Troca de Óleo");
        Servico outroServico = novoServico(2L, "Alinhamento");
        Servico dadosAtualizados = new Servico(1L, "Alinhamento", "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(servicoExistente));
        when(repositoryPort.buscarPorNome("Alinhamento")).thenReturn(Optional.of(outroServico));

        assertThrows(ResourceAlreadyExistsException.class, () -> servicoService.atualizarServico(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Servico.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao atualizar sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoAtualizarSemId() {
        Servico semId = novoServico(null, "Troca de Óleo");

        assertThrows(IllegalArgumentException.class, () -> servicoService.atualizarServico(semId));
        verify(repositoryPort, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("Deve excluir um serviço com sucesso")
    void deveExcluirServico() {
        Servico servicoExistente = novoServico(1L, "Troca de Óleo");
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(servicoExistente));

        servicoService.excluirServico(1L);

        verify(repositoryPort, times(1)).excluirPorId(1L);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao excluir serviço inexistente")
    void deveLancarResourceNotFoundExceptionAoExcluirServicoInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicoService.excluirServico(1L));
        verify(repositoryPort, never()).excluirPorId(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao excluir sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoExcluirSemId() {
        assertThrows(IllegalArgumentException.class, () -> servicoService.excluirServico(null));
        verify(repositoryPort, never()).buscarPorId(any());
    }
}
