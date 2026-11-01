package br.com.fiap.application.services;

import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.domain.valueobjects.TipoServico;
import br.com.fiap.ports.in.CriarOrdemServicoCommand;
import br.com.fiap.ports.in.PecaUseCase;
import br.com.fiap.ports.in.ServicoUseCase;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.StatusOsNotification;
import br.com.fiap.ports.out.StatusOsNotificationPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
class StatusOsNotificationServiceTest {
    @Mock OrdemServicoRepositoryPort repository;
    @Mock ClienteRepositoryPort clientes;
    @Mock VeiculoRepositoryPort veiculos;
    @Mock ServicoUseCase servicos;
    @Mock PecaUseCase pecas;
    @Mock StatusOsNotificationPort notifications;
    @InjectMocks OrdemServicoService service;

    private Cliente cliente(String email) {
        return new Cliente(1L,"Cliente","12345678909",email,"11999999999");
    }
    private OrdemServico os(StatusOS status) {
        var zero=new Preco(0);
        return new OrdemServico(42L,1L,2L,status,LocalDateTime.now(),null,null,"obs",zero,zero,zero,null,null,"chave",null,null);
    }
    private Servico servico() {
        return new Servico(3L,"Revisao","Revisao",new Preco(100),TipoServico.REVISAO,60);
    }
    private OrdemServico budget() {
        var os=os(StatusOS.EM_DIAGNOSTICO);
        os.adicionarServico(servico(),1);
        os.enviarOrcamento();
        return os;
    }
    private void persistAndFindEmail() {
        when(repository.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente("cliente@test.invalid")));
    }
    private void assertNotificationAfterSave(StatusOS status) {
        var ordered=inOrder(repository,notifications);
        ordered.verify(repository).salvar(any());
        ordered.verify(notifications).notificar(new StatusOsNotification(42L,status,"cliente@test.invalid"));
        verifyNoMoreInteractions(notifications);
    }

    @Test
    void creationNotifiesRecebidaUsingThePersistedIdentifier() {
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente("cliente@test.invalid")));
        when(veiculos.buscarPorId(2L)).thenReturn(Optional.of(new Veiculo(2L,"Toyota","Corolla",2022,"ABC1D23",1L)));
        when(repository.salvar(any())).thenAnswer(invocation -> {
            OrdemServico os=invocation.getArgument(0);
            return new OrdemServico(42L,os.getClienteId(),os.getVeiculoId(),os.getStatus(),os.getDataAbertura(),null,null,
                    os.getObservacoes(),os.getValorServicos(),os.getValorPecas(),os.getValorTotal(),os.getServicos(),os.getPecas(),os.getChaveAcesso(),null,null);
        });
        assertEquals(42L,service.criarOS(new CriarOrdemServicoCommand(1L,2L,null,List.of(),List.of())).getId());
        assertNotificationAfterSave(StatusOS.RECEBIDA);
    }

    @Test
    void diagnosticNotifiesThePersistedNewStatus() {
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os(StatusOS.RECEBIDA)));
        persistAndFindEmail();
        service.realizarDiagnostico(42L,List.of(),List.of(),null);
        assertNotificationAfterSave(StatusOS.EM_DIAGNOSTICO);
    }

    @Test
    void sendingBudgetNotifiesWaitingForApproval() {
        var os=os(StatusOS.EM_DIAGNOSTICO);
        os.adicionarServico(servico(),1);
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os));
        persistAndFindEmail();
        service.enviarOrcamento(42L);
        assertNotificationAfterSave(StatusOS.AGUARDANDO_APROVACAO);
    }

    @ParameterizedTest
    @ValueSource(booleans={true,false})
    void decisionNotifiesOnceAndAnIdenticalReplayDoesNotNotify(boolean approved) {
        var os=budget();
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os));
        persistAndFindEmail();
        service.decidirOrcamento(42L,"chave",1L,approved);
        service.decidirOrcamento(42L,"chave",1L,approved);
        assertNotificationAfterSave(approved?StatusOS.EM_EXECUCAO:StatusOS.EM_DIAGNOSTICO);
        verify(repository,times(1)).salvar(any());
        verify(clientes,times(1)).buscarPorId(1L);
    }

    @ParameterizedTest
    @CsvSource({"RECEBIDA,EM_DIAGNOSTICO","EM_DIAGNOSTICO,AGUARDANDO_APROVACAO","EM_EXECUCAO,FINALIZADA","FINALIZADA,ENTREGUE"})
    void administrativeStatusChangesNotifyTheResult(StatusOS initial,StatusOS target) {
        var os=os(initial);
        if(initial==StatusOS.EM_DIAGNOSTICO)os.adicionarServico(servico(),1);
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os));
        persistAndFindEmail();
        service.atualizarStatus(42L,target);
        assertNotificationAfterSave(target);
    }

    @Test
    void invalidTransitionDoesNotSaveOrNotify() {
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(budget()));
        assertThrows(IllegalStateException.class,()->service.atualizarStatus(42L,StatusOS.EM_EXECUCAO));
        verify(repository,never()).salvar(any());
        verifyNoInteractions(notifications,clientes);
    }

    @Test
    void invalidDecisionKeyDoesNotSaveOrNotify() {
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(budget()));
        assertThrows(IllegalArgumentException.class,()->service.decidirOrcamento(42L,"wrong",1L,true));
        verify(repository,never()).salvar(any());
        verifyNoInteractions(notifications,clientes);
    }

    @Test
    void rejectedSaveCannotPublishANotification() {
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os(StatusOS.EM_EXECUCAO)));
        when(repository.salvar(any())).thenThrow(new IllegalStateException("Persistence failed"));
        assertThrows(IllegalStateException.class,()->service.atualizarStatus(42L,StatusOS.FINALIZADA));
        verifyNoInteractions(notifications);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={" ","\t"})
    void missingCustomerEmailDoesNotInventARecipient(String email) {
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os(StatusOS.EM_EXECUCAO)));
        when(repository.salvar(any())).thenAnswer(invocation->invocation.getArgument(0));
        when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente(email)));
        assertEquals(StatusOS.FINALIZADA,service.atualizarStatus(42L,StatusOS.FINALIZADA).getStatus());
        verifyNoInteractions(notifications);
    }

    @Test
    void addingItemsWithoutChangingStatusDoesNotPublish() {
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os(StatusOS.EM_DIAGNOSTICO)));
        when(repository.salvar(any())).thenAnswer(invocation->invocation.getArgument(0));
        when(servicos.buscarPorId(3L)).thenReturn(servico());
        when(pecas.buscarPorId(4L)).thenReturn(new Peca(4L,"Filtro","Filtro","F001",new Preco(20),10,1));
        service.adicionarServico(42L,3L,1);
        service.adicionarPeca(42L,4L,1);
        verifyNoInteractions(notifications,clientes);
    }

    @Test
    void customerRatingWithoutChangingStatusDoesNotPublish() {
        when(repository.buscarPorIdParaAtualizacao(42L)).thenReturn(Optional.of(os(StatusOS.ENTREGUE)));
        when(repository.salvar(any())).thenAnswer(invocation->invocation.getArgument(0));
        service.avaliarServico(42L,"chave",5,"Bom");
        verifyNoInteractions(notifications,clientes);
    }
}
