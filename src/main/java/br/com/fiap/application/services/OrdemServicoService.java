package br.com.fiap.application.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.entities.PecaOS;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.ItemDiagnostico;
import br.com.fiap.ports.in.OrdemServicoUseCase;
import br.com.fiap.ports.in.PecaUseCase;
import br.com.fiap.ports.in.ServicoUseCase;
import br.com.fiap.ports.in.TempoMedioExecucao;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

@Service
public class OrdemServicoService implements OrdemServicoUseCase {

    private final OrdemServicoRepositoryPort osRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final VeiculoRepositoryPort veiculoRepositoryPort;
    private final ServicoUseCase servicoUseCase;
    private final PecaUseCase pecaUseCase;

    public OrdemServicoService(OrdemServicoRepositoryPort osRepositoryPort,
                                ClienteRepositoryPort clienteRepositoryPort,
                                VeiculoRepositoryPort veiculoRepositoryPort,
                                ServicoUseCase servicoUseCase,
                                PecaUseCase pecaUseCase) {
        this.osRepositoryPort = osRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.veiculoRepositoryPort = veiculoRepositoryPort;
        this.servicoUseCase = servicoUseCase;
        this.pecaUseCase = pecaUseCase;
    }

    @Override
    @Transactional
    public OrdemServico criarOS(OrdemServico os) {
        clienteRepositoryPort.buscarPorId(os.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + os.getClienteId()));

        veiculoRepositoryPort.buscarPorId(os.getVeiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com ID: " + os.getVeiculoId()));

        OrdemServico novaOS = new OrdemServico(
            null,
            os.getClienteId(),
            os.getVeiculoId(),
            StatusOS.RECEBIDA,
            LocalDateTime.now(),
            null,
            null,
            os.getObservacoes(),
            new Preco(BigDecimal.ZERO),
            new Preco(BigDecimal.ZERO),
            new Preco(BigDecimal.ZERO),
            null,
            null,
            UUID.randomUUID().toString(),
            null,
            null
        );

        return osRepositoryPort.salvar(novaOS);
    }

    @Override
    @Transactional
    public OrdemServico realizarDiagnostico(Long osId, List<ItemDiagnostico> servicos, List<ItemDiagnostico> pecas, LocalDateTime dataPrevistaEntrega) {
        OrdemServico os = osRepositoryPort.buscarPorId(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + osId));

        os.transicionarPara(StatusOS.EM_DIAGNOSTICO);

        if (dataPrevistaEntrega != null) {
            os.definirDataPrevistaEntrega(dataPrevistaEntrega);
        }

        if (servicos != null) {
            for (ItemDiagnostico item : servicos) {
                Servico servico = servicoUseCase.buscarPorId(item.id());
                os.adicionarServico(servico, item.quantidade());
            }
        }

        if (pecas != null) {
            for (ItemDiagnostico item : pecas) {
                Peca peca = pecaUseCase.buscarPorId(item.id());
                os.adicionarPeca(peca, item.quantidade());
            }
        }

        return osRepositoryPort.salvar(os);
    }

    @Override
    @Transactional
    public OrdemServico adicionarServico(Long osId, Long servicoId, int quantidade) {
        OrdemServico os = osRepositoryPort.buscarPorId(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + osId));

        Servico servico = servicoUseCase.buscarPorId(servicoId);
        os.adicionarServico(servico, quantidade);

        return osRepositoryPort.salvar(os);
    }

    @Override
    @Transactional
    public OrdemServico adicionarPeca(Long osId, Long pecaId, int quantidade) {
        OrdemServico os = osRepositoryPort.buscarPorId(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + osId));

        Peca peca = pecaUseCase.buscarPorId(pecaId);
        os.adicionarPeca(peca, quantidade);

        return osRepositoryPort.salvar(os);
    }

    @Override
    public OrdemServico buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da OS não pode ser nulo.");
        }
        return osRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));
    }

    @Override
    public List<OrdemServico> listarTodas() {
        return osRepositoryPort.buscarTodas();
    }

    @Override
    public List<OrdemServico> listarPorCliente(Long clienteId) {
        clienteRepositoryPort.buscarPorId(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));

        return osRepositoryPort.buscarPorClienteId(clienteId);
    }

    @Override
    public List<OrdemServico> listarPorStatus(StatusOS status) {
        return osRepositoryPort.buscarPorStatus(status);
    }

    @Override
    public List<OrdemServico> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return osRepositoryPort.buscarPorPeriodo(inicio, fim);
    }

    @Override
    @Transactional
    public OrdemServico atualizarStatus(Long id, StatusOS novoStatus) {
        OrdemServico os = osRepositoryPort.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));

        os.transicionarPara(novoStatus);

        if (novoStatus == StatusOS.EM_EXECUCAO) {
            baixarEstoqueDosItens(os);
        }

        return osRepositoryPort.salvar(os);
    }

    @Override
    @Transactional
    public OrdemServico enviarOrcamento(Long id) {
        OrdemServico os = osRepositoryPort.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));

        os.enviarOrcamento();
        return osRepositoryPort.salvar(os);
    }

    @Override
    @Transactional
    public OrdemServico aprovarOrcamento(Long id, String chave) {
        OrdemServico os = osRepositoryPort.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));

        os.aprovarOrcamento(chave);
        baixarEstoqueDosItens(os);

        return osRepositoryPort.salvar(os);
    }

    @Override
    @Transactional
    public OrdemServico avaliarServico(Long id, String chave, int nota, String comentario) {
        OrdemServico os = osRepositoryPort.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));

        os.validarChaveAcesso(chave);
        os.avaliarServico(nota, comentario);

        return osRepositoryPort.salvar(os);
    }

    @Override
    public OrdemServico buscarParaAcompanhamento(Long id, String chave) {
        OrdemServico os = osRepositoryPort.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));

        os.validarChaveAcesso(chave);
        return os;
    }

    private void baixarEstoqueDosItens(OrdemServico os) {
        for (PecaOS item : os.getPecas()) {
            pecaUseCase.baixarEstoque(item.getPecaId(), item.getQuantidade());
        }
    }

    @Override
    public TempoMedioExecucao calcularTempoMedioExecucao() {
        List<OrdemServico> concluidas = osRepositoryPort.buscarTodas().stream()
                .filter(os -> os.getDataConclusao() != null)
                .toList();

        if (concluidas.isEmpty()) {
            return new TempoMedioExecucao(0.0, 0);
        }

        double mediaMinutos = concluidas.stream()
                .mapToLong(os -> Duration.between(os.getDataAbertura(), os.getDataConclusao()).toMinutes())
                .average()
                .orElse(0.0);

        return new TempoMedioExecucao(mediaMinutos, concluidas.size());
    }
}
