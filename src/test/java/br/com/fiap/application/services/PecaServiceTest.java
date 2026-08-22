package br.com.fiap.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.ports.out.PecaRepositoryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock
    private PecaRepositoryPort repositoryPort;

    @InjectMocks
    private PecaService pecaService;

    private Peca novaPeca(Long id, String codigo, int quantidadeEstoque, int estoqueMinimo) {
        return new Peca(id, "Filtro de Óleo", "Descrição", codigo, new Preco(BigDecimal.valueOf(45.90)), quantidadeEstoque, estoqueMinimo);
    }

    @Test
    @DisplayName("Deve cadastrar uma peça com sucesso")
    void deveCadastrarPeca() {
        Peca peca = novaPeca(null, "FIL001", 50, 10);
        Peca pecaSalva = novaPeca(1L, "FIL001", 50, 10);

        when(repositoryPort.buscarPorCodigo("FIL001")).thenReturn(Optional.empty());
        when(repositoryPort.salvar(any(Peca.class))).thenReturn(pecaSalva);

        Peca resultado = pecaService.cadastrarPeca(peca);

        assertNotNull(resultado.getId());
        assertEquals("FIL001", resultado.getCodigo());
        verify(repositoryPort, times(1)).salvar(any(Peca.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao cadastrar com código já existente")
    void deveLancarResourceAlreadyExistsExceptionQuandoCodigoExiste() {
        Peca peca = novaPeca(null, "FIL001", 50, 10);
        when(repositoryPort.buscarPorCodigo("FIL001")).thenReturn(Optional.of(peca));

        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> pecaService.cadastrarPeca(peca));

        assertEquals("Peça com código informado já existe.", exception.getMessage());
        verify(repositoryPort, never()).salvar(any(Peca.class));
    }

    @Test
    @DisplayName("Deve listar todas as peças cadastradas")
    void deveListarTodasAsPecas() {
        Peca peca = novaPeca(1L, "FIL001", 50, 10);
        when(repositoryPort.buscarTodas()).thenReturn(List.of(peca));

        List<Peca> resultado = pecaService.listarTodas();

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve buscar peça por ID com sucesso")
    void deveBuscarPorId() {
        Peca peca = novaPeca(1L, "FIL001", 50, 10);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(peca));

        Peca resultado = pecaService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("FIL001", resultado.getCodigo());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar por ID inexistente")
    void deveLancarResourceNotFoundExceptionAoBuscarPorIdInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pecaService.buscarPorId(1L));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao buscar por ID nulo")
    void deveLancarIllegalArgumentExceptionAoBuscarPorIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> pecaService.buscarPorId(null));
    }

    @Test
    @DisplayName("Deve buscar peça por código com sucesso")
    void deveBuscarPorCodigo() {
        Peca peca = novaPeca(1L, "FIL001", 50, 10);
        when(repositoryPort.buscarPorCodigo("FIL001")).thenReturn(Optional.of(peca));

        Peca resultado = pecaService.buscarPorCodigo("FIL001");

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar por código inexistente")
    void deveLancarResourceNotFoundExceptionAoBuscarPorCodigoInexistente() {
        when(repositoryPort.buscarPorCodigo("FIL001")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pecaService.buscarPorCodigo("FIL001"));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao buscar por código vazio")
    void deveLancarIllegalArgumentExceptionAoBuscarPorCodigoVazio() {
        assertThrows(IllegalArgumentException.class, () -> pecaService.buscarPorCodigo(" "));
        assertThrows(IllegalArgumentException.class, () -> pecaService.buscarPorCodigo(null));
    }

    @Test
    @DisplayName("Deve baixar estoque com sucesso")
    void deveBaixarEstoque() {
        Peca peca = novaPeca(1L, "FIL001", 50, 10);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(peca));

        pecaService.baixarEstoque(1L, 5);

        verify(repositoryPort, times(1)).salvar(peca);
        assertEquals(45, peca.getQuantidadeEstoque());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao baixar estoque de peça inexistente")
    void deveLancarResourceNotFoundExceptionAoBaixarEstoqueDePecaInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pecaService.baixarEstoque(1L, 5));
        verify(repositoryPort, never()).salvar(any(Peca.class));
    }

    @Test
    @DisplayName("Deve repor estoque com sucesso")
    void deveReporEstoque() {
        Peca peca = novaPeca(1L, "FIL001", 50, 10);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(peca));

        pecaService.reporEstoque(1L, 10);

        verify(repositoryPort, times(1)).salvar(peca);
        assertEquals(60, peca.getQuantidadeEstoque());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao repor estoque de peça inexistente")
    void deveLancarResourceNotFoundExceptionAoReporEstoqueDePecaInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pecaService.reporEstoque(1L, 10));
        verify(repositoryPort, never()).salvar(any(Peca.class));
    }

    @Test
    @DisplayName("Deve listar peças com estoque baixo")
    void deveListarEstoqueBaixo() {
        Peca peca = novaPeca(1L, "FIL001", 5, 10);
        when(repositoryPort.buscarEstoqueBaixo(0)).thenReturn(List.of(peca));

        List<Peca> resultado = pecaService.listarEstoqueBaixo();

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve atualizar uma peça preservando a quantidade em estoque")
    void deveAtualizarPecaPreservandoEstoque() {
        Peca pecaExistente = novaPeca(1L, "FIL001", 50, 10);
        Peca dadosAtualizados = new Peca(1L, "Filtro de Óleo Premium", "Nova descrição", "FIL001", new Preco(BigDecimal.valueOf(55.00)), null, 15);

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(pecaExistente));
        when(repositoryPort.buscarPorCodigo("FIL001")).thenReturn(Optional.of(pecaExistente));
        when(repositoryPort.salvar(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Peca resultado = pecaService.atualizarPeca(dadosAtualizados);

        assertEquals("Filtro de Óleo Premium", resultado.getNome());
        assertEquals(15, resultado.getEstoqueMinimo());
        assertEquals(50, resultado.getQuantidadeEstoque());
        verify(repositoryPort, times(1)).salvar(any(Peca.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao atualizar peça inexistente")
    void deveLancarResourceNotFoundExceptionAoAtualizarPecaInexistente() {
        Peca dadosAtualizados = novaPeca(1L, "FIL001", 0, 10);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pecaService.atualizarPeca(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Peca.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException ao atualizar para o código de outra peça")
    void deveLancarResourceAlreadyExistsExceptionAoAtualizarComCodigoDeOutraPeca() {
        Peca pecaExistente = novaPeca(1L, "FIL001", 50, 10);
        Peca outraPeca = novaPeca(2L, "FIL002", 20, 5);
        Peca dadosAtualizados = new Peca(1L, "Filtro de Óleo", "Descrição", "FIL002", new Preco(BigDecimal.valueOf(45.90)), null, 10);

        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(pecaExistente));
        when(repositoryPort.buscarPorCodigo("FIL002")).thenReturn(Optional.of(outraPeca));

        assertThrows(ResourceAlreadyExistsException.class, () -> pecaService.atualizarPeca(dadosAtualizados));
        verify(repositoryPort, never()).salvar(any(Peca.class));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao atualizar sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoAtualizarSemId() {
        Peca semId = novaPeca(null, "FIL001", 0, 10);

        assertThrows(IllegalArgumentException.class, () -> pecaService.atualizarPeca(semId));
        verify(repositoryPort, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("Deve excluir uma peça com sucesso")
    void deveExcluirPeca() {
        Peca pecaExistente = novaPeca(1L, "FIL001", 50, 10);
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.of(pecaExistente));

        pecaService.excluirPeca(1L);

        verify(repositoryPort, times(1)).excluirPorId(1L);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao excluir peça inexistente")
    void deveLancarResourceNotFoundExceptionAoExcluirPecaInexistente() {
        when(repositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pecaService.excluirPeca(1L));
        verify(repositoryPort, never()).excluirPorId(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao excluir sem informar o ID")
    void deveLancarIllegalArgumentExceptionAoExcluirSemId() {
        assertThrows(IllegalArgumentException.class, () -> pecaService.excluirPeca(null));
        verify(repositoryPort, never()).buscarPorId(any());
    }
}
