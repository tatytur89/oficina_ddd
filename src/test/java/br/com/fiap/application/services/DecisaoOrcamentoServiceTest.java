package br.com.fiap.application.services;

import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.*;
import br.com.fiap.domain.valueobjects.*;
import br.com.fiap.ports.in.*;
import br.com.fiap.ports.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisaoOrcamentoServiceTest {
    @Mock OrdemServicoRepositoryPort repository;
    @Mock ClienteRepositoryPort clientes;
    @Mock VeiculoRepositoryPort veiculos;
    @Mock ServicoUseCase servicos;
    @Mock PecaUseCase pecas;
    @InjectMocks OrdemServicoService service;

    private OrdemServico aguardando() {
        var preco = new Preco(50);
        return new OrdemServico(1L, 1L, 2L, StatusOS.AGUARDANDO_APROVACAO, LocalDateTime.now(), null, null,
                "obs", preco, preco, new Preco(100),
                List.of(new ServicoOS(3L, "Revisao", 1, preco, preco)),
                List.of(new PecaOS(4L, "Filtro", "F001", 2, new Preco(25), preco)),
                "chave", null, null, 7L, null);
    }

    @Test
    void aprovaComLockBaixaEstoqueUmaVezESalvaDecisao() {
        var os = aguardando();
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(os));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        assertSame(os, service.decidirOrcamento(1L, "chave", 7L, true));
        assertSame(os, service.decidirOrcamento(1L, "chave", 7L, true));

        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
        assertEquals(Boolean.TRUE, os.getDecisaoOrcamento());
        verify(repository, times(2)).buscarPorIdParaAtualizacao(1L);
        verify(repository, never()).buscarPorId(anyLong());
        verify(repository, times(1)).salvar(os);
        verify(pecas, times(1)).baixarEstoque(4L, 2);
    }

    @Test
    void recusaComLockPreservaItensENaoBaixaEstoque() {
        var os = aguardando();
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(os));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.decidirOrcamento(1L, "chave", 7L, false);
        service.decidirOrcamento(1L, "chave", 7L, false);

        assertEquals(StatusOS.EM_DIAGNOSTICO, os.getStatus());
        assertEquals(Boolean.FALSE, os.getDecisaoOrcamento());
        assertEquals(1, os.getPecas().size());
        assertEquals(1, os.getServicos().size());
        verify(repository, times(1)).salvar(os);
        verifyNoInteractions(pecas);
    }

    @Test
    void rejeitaVersaoObsoletaSemSalvarOuBaixarEstoque() {
        var os = aguardando();
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(os));
        assertThrows(IllegalStateException.class, () -> service.decidirOrcamento(1L, "chave", 6L, true));
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
        verify(repository, never()).salvar(any());
        verifyNoInteractions(pecas);
    }

    @Test
    void rejeitaChaveInvalidaSemSalvarOuBaixarEstoque() {
        var os = aguardando();
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(os));
        assertThrows(IllegalArgumentException.class, () -> service.decidirOrcamento(1L, "errada", 7L, true));
        verify(repository, never()).salvar(any());
        verifyNoInteractions(pecas);
    }

    @Test
    void retornaNaoEncontradoSemSalvarOuBaixarEstoque() {
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.decidirOrcamento(1L, "chave", 7L, true));
        verify(repository, never()).salvar(any());
        verifyNoInteractions(pecas);
    }

    @Test
    void recusaEAprovacaoContraditoriasNaoAlteramPrimeiraDecisao() {
        var os = aguardando();
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(os));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        service.decidirOrcamento(1L, "chave", 7L, false);
        assertThrows(IllegalStateException.class, () -> service.decidirOrcamento(1L, "chave", 7L, true));
        assertEquals(Boolean.FALSE, os.getDecisaoOrcamento());
        verify(repository, times(1)).salvar(any());
        verifyNoInteractions(pecas);
    }

    @Test
    void naoSalvaSeBaixaDeEstoqueFalhar() {
        var os = aguardando();
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(os));
        doThrow(new IllegalStateException("Estoque insuficiente")).when(pecas).baixarEstoque(4L, 2);
        assertThrows(IllegalStateException.class, () -> service.decidirOrcamento(1L, "chave", 7L, true));
        verify(repository, never()).salvar(any());
    }

    @Test
    void aprovacaoLegadaTambemUsaLockESemConsumoDuplicado() {
        var os = aguardando();
        when(repository.buscarPorIdParaAtualizacao(1L)).thenReturn(Optional.of(os));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        service.aprovarOrcamento(1L, "chave");
        service.aprovarOrcamento(1L, "chave");
        verify(repository, times(2)).buscarPorIdParaAtualizacao(1L);
        verify(repository, times(1)).salvar(os);
        verify(pecas, times(1)).baixarEstoque(4L, 2);
    }
}
