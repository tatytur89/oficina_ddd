package br.com.fiap.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.domain.valueobjects.TipoServico;
import br.com.fiap.ports.in.ItemQuantidade;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.PecaRepositoryPort;
import br.com.fiap.ports.out.ServicoRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepositoryPort osRepositoryPort;

    @Mock
    private ClienteRepositoryPort clienteRepositoryPort;

    @Mock
    private VeiculoRepositoryPort veiculoRepositoryPort;

    @Mock
    private ServicoRepositoryPort servicoRepositoryPort;

    @Mock
    private PecaRepositoryPort pecaRepositoryPort;

    @InjectMocks
    private OrdemServicoService ordemServicoService;

    private static final Cliente CLIENTE = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
    private static final Veiculo VEICULO = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);

    private OrdemServico novaOSMinima() {
        return new OrdemServico(null, 1L, 1L, null, null, null, null, "Barulho no freio", null, null, null, null, null);
    }

    @Test
    @DisplayName("Deve criar uma OS com serviços e peças, calculando o orçamento corretamente")
    void deveCriarOSComServicosEPecas() {
        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);
        Peca peca = new Peca(1L, "Filtro de Óleo", "Descrição", "FIL001", new Preco(BigDecimal.valueOf(45.90)), 50, 10);

        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(veiculoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(VEICULO));
        when(servicoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(servico));
        when(pecaRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(peca));
        when(osRepositoryPort.salvar(any(OrdemServico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico resultado = ordemServicoService.criarOS(
                novaOSMinima(),
                List.of(new ItemQuantidade(1L, 1)),
                List.of(new ItemQuantidade(1L, 2))
        );

        assertEquals(StatusOS.RECEBIDA, resultado.getStatus());
        assertEquals(1, resultado.getServicos().size());
        assertEquals(1, resultado.getPecas().size());
        assertEquals(0, new BigDecimal("150.00").compareTo(resultado.getValorServicos().getValor()));
        assertEquals(0, new BigDecimal("91.80").compareTo(resultado.getValorPecas().getValor()));
        assertEquals(0, new BigDecimal("241.80").compareTo(resultado.getValorTotal().getValor()));
        verify(osRepositoryPort, times(1)).salvar(any(OrdemServico.class));
    }

    @Test
    @DisplayName("Deve criar uma OS sem serviços e peças informados")
    void deveCriarOSSemServicosEPecas() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(veiculoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(VEICULO));
        when(osRepositoryPort.salvar(any(OrdemServico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico resultado = ordemServicoService.criarOS(novaOSMinima(), null, null);

        assertTrue(resultado.getServicos().isEmpty());
        assertTrue(resultado.getPecas().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar OS com cliente inexistente")
    void deveLancarResourceNotFoundExceptionAoCriarOSComClienteInexistente() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ordemServicoService.criarOS(novaOSMinima(), null, null));
        verify(osRepositoryPort, never()).salvar(any(OrdemServico.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar OS com veículo inexistente")
    void deveLancarResourceNotFoundExceptionAoCriarOSComVeiculoInexistente() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(veiculoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ordemServicoService.criarOS(novaOSMinima(), null, null));
        verify(osRepositoryPort, never()).salvar(any(OrdemServico.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar OS com serviço inexistente")
    void deveLancarResourceNotFoundExceptionAoCriarOSComServicoInexistente() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(veiculoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(VEICULO));
        when(servicoRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ordemServicoService.criarOS(novaOSMinima(), List.of(new ItemQuantidade(99L, 1)), null));
        verify(osRepositoryPort, never()).salvar(any(OrdemServico.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar OS com peça inexistente")
    void deveLancarResourceNotFoundExceptionAoCriarOSComPecaInexistente() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(veiculoRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(VEICULO));
        when(pecaRepositoryPort.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ordemServicoService.criarOS(novaOSMinima(), null, List.of(new ItemQuantidade(99L, 1))));
        verify(osRepositoryPort, never()).salvar(any(OrdemServico.class));
    }

    @Test
    @DisplayName("Deve listar OS por cliente")
    void deveListarPorCliente() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, null, null, null, "obs", new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null);
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(CLIENTE));
        when(osRepositoryPort.buscarPorClienteId(1L)).thenReturn(List.of(os));

        List<OrdemServico> resultado = ordemServicoService.listarPorCliente(1L);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao listar OS de cliente inexistente")
    void deveLancarResourceNotFoundExceptionAoListarPorClienteInexistente() {
        when(clienteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ordemServicoService.listarPorCliente(1L));
        verify(osRepositoryPort, never()).buscarPorClienteId(any());
    }

    @Test
    @DisplayName("Deve buscar OS por ID com sucesso")
    void deveBuscarPorId() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, null, null, null, "obs", new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null);
        when(osRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(os));

        OrdemServico resultado = ordemServicoService.buscarPorId(1L);

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar OS por ID inexistente")
    void deveLancarResourceNotFoundExceptionAoBuscarPorIdInexistente() {
        when(osRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ordemServicoService.buscarPorId(1L));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException ao buscar OS por ID nulo")
    void deveLancarIllegalArgumentExceptionAoBuscarPorIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> ordemServicoService.buscarPorId(null));
    }

    @Test
    @DisplayName("Deve atualizar o status da OS com sucesso")
    void deveAtualizarStatus() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, null, null, null, "obs", new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null);
        when(osRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(os));
        when(osRepositoryPort.salvar(any(OrdemServico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico resultado = ordemServicoService.atualizarStatus(1L, StatusOS.EM_ANDAMENTO);

        assertEquals(StatusOS.EM_ANDAMENTO, resultado.getStatus());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao atualizar status de OS inexistente")
    void deveLancarResourceNotFoundExceptionAoAtualizarStatusDeOSInexistente() {
        when(osRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ordemServicoService.atualizarStatus(1L, StatusOS.EM_ANDAMENTO));
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException ao tentar uma transição de status inválida")
    void deveLancarIllegalStateExceptionAoTransicionarStatusInvalido() {
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.ENTREGUE, null, null, null, "obs", new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null);
        when(osRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(os));

        assertThrows(IllegalStateException.class, () -> ordemServicoService.atualizarStatus(1L, StatusOS.EM_EXECUCAO));
        verify(osRepositoryPort, never()).salvar(any(OrdemServico.class));
    }

    @Test
    @DisplayName("Deve enviar o orçamento com sucesso")
    void deveEnviarOrcamento() {
        Servico servico = new Servico(1L, "Troca de Óleo", "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);
        OrdemServico os = new OrdemServico(1L, 1L, 1L, StatusOS.EM_ANDAMENTO, null, null, null, "obs", new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null);
        os.adicionarServico(servico, 1);

        when(osRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(os));
        when(osRepositoryPort.salvar(any(OrdemServico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdemServico resultado = ordemServicoService.enviarOrcamento(1L);

        assertEquals(StatusOS.AGUARDANDO_APROVACAO, resultado.getStatus());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao enviar orçamento de OS inexistente")
    void deveLancarResourceNotFoundExceptionAoEnviarOrcamentoDeOSInexistente() {
        when(osRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ordemServicoService.enviarOrcamento(1L));
    }
}
