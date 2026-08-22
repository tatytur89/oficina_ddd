package br.com.fiap.application.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.entities.PecaOS;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.ItemQuantidade;
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
    public OrdemServico criarOS(OrdemServico os, List<ItemQuantidade> servicosSolicitados, List<ItemQuantidade> pecasSolicitadas) {
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
            os.getDataPrevistaEntrega(),
            null,
            os.getObservacoes(),
            new Preco(BigDecimal.ZERO),
            new Preco(BigDecimal.ZERO),
            new Preco(BigDecimal.ZERO),
            null,
            null
        );

        if (servicosSolicitados != null) {
            for (ItemQuantidade item : servicosSolicitados) {
                Servico servico = servicoUseCase.buscarPorId(item.id());
                novaOS.adicionarServico(servico, item.quantidade());
            }
        }

        if (pecasSolicitadas != null) {
            for (ItemQuantidade item : pecasSolicitadas) {
                Peca peca = pecaUseCase.buscarPorId(item.id());
                novaOS.adicionarPeca(peca, item.quantidade());
            }
        }

        return osRepositoryPort.salvar(novaOS);
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
            for (PecaOS item : os.getPecas()) {
                pecaUseCase.baixarEstoque(item.getPecaId(), item.getQuantidade());
            }
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
