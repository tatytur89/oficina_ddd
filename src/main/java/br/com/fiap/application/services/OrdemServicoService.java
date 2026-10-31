package br.com.fiap.application.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.entities.PecaOS;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.in.CriarOrdemServicoCommand;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.ItemDiagnostico;
import br.com.fiap.ports.in.OrdemServicoUseCase;
import br.com.fiap.ports.in.PecaUseCase;
import br.com.fiap.ports.in.ServicoUseCase;
import br.com.fiap.ports.in.TempoMedioExecucao;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;
import br.com.fiap.ports.out.StatusOsNotification;
import br.com.fiap.ports.out.StatusOsNotificationPort;

@Service
public class OrdemServicoService implements OrdemServicoUseCase {

    private final OrdemServicoRepositoryPort osRepositoryPort;
    private final ClienteRepositoryPort clienteRepositoryPort;
    private final VeiculoRepositoryPort veiculoRepositoryPort;
    private final ServicoUseCase servicoUseCase;
    private final PecaUseCase pecaUseCase;
    private final StatusOsNotificationPort notificationPort;

    public OrdemServicoService(OrdemServicoRepositoryPort osRepositoryPort,
                                ClienteRepositoryPort clienteRepositoryPort,
                                VeiculoRepositoryPort veiculoRepositoryPort,
                                ServicoUseCase servicoUseCase,
                                PecaUseCase pecaUseCase,
                                StatusOsNotificationPort notificationPort) {
        this.osRepositoryPort = osRepositoryPort;
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.veiculoRepositoryPort = veiculoRepositoryPort;
        this.servicoUseCase = servicoUseCase;
        this.pecaUseCase = pecaUseCase;
        this.notificationPort = notificationPort;
    }

    @Override
    @Transactional
    public OrdemServico criarOS(CriarOrdemServicoCommand command) {
        Cliente cliente = clienteRepositoryPort.buscarPorId(command.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + command.clienteId()));

        Veiculo veiculo = veiculoRepositoryPort.buscarPorId(command.veiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado com ID: " + command.veiculoId()));
        if (!command.clienteId().equals(veiculo.getClienteId())) {
            throw new IllegalArgumentException("O veículo informado não pertence ao cliente da OS.");
        }

        OrdemServico novaOS = OrdemServico.abrir(command.clienteId(), command.veiculoId(), command.observacoes());
        for (ItemDiagnostico item : command.servicos()) {
            novaOS.registrarServicoSolicitado(servicoUseCase.buscarPorId(item.id()), item.quantidade());
        }
        for (ItemDiagnostico item : command.pecas()) {
            novaOS.registrarPecaSolicitada(pecaUseCase.buscarPorId(item.id()), item.quantidade());
        }

        OrdemServico salva = osRepositoryPort.salvar(novaOS);
        notificarStatus(salva, cliente);
        return salva;
    }

    @Override
    @Transactional
    public OrdemServico realizarDiagnostico(Long osId, List<ItemDiagnostico> servicos, List<ItemDiagnostico> pecas, LocalDateTime dataPrevistaEntrega) {
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + osId));

        StatusOS anterior = os.getStatus();
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

        return salvarComAvisoDeStatus(os, anterior);
    }

    @Override
    @Transactional
    public OrdemServico adicionarServico(Long osId, Long servicoId, int quantidade) {
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(osId)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + osId));

        Servico servico = servicoUseCase.buscarPorId(servicoId);
        os.adicionarServico(servico, quantidade);

        return osRepositoryPort.salvar(os);
    }

    @Override
    @Transactional
    public OrdemServico adicionarPeca(Long osId, Long pecaId, int quantidade) {
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(osId)
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
        return osRepositoryPort.buscarTodas().stream()
                .filter(os -> os.getStatus() != StatusOS.FINALIZADA && os.getStatus() != StatusOS.ENTREGUE)
                .sorted(Comparator.comparingInt((OrdemServico os) -> prioridadeNaFila(os.getStatus()))
                        .thenComparing(OrdemServico::getDataAbertura,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OrdemServico::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private int prioridadeNaFila(StatusOS status) {
        return switch (status) {
            case EM_EXECUCAO -> 0;
            case AGUARDANDO_APROVACAO -> 1;
            case EM_DIAGNOSTICO -> 2;
            case RECEBIDA -> 3;
            default -> throw new IllegalArgumentException("Status fora da fila operacional: " + status);
        };
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
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));

        StatusOS anterior = os.getStatus();
        os.transicionarPara(novoStatus);

        return salvarComAvisoDeStatus(os, anterior);
    }

    @Override
    @Transactional
    public OrdemServico enviarOrcamento(Long id) {
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));

        StatusOS anterior = os.getStatus();
        os.enviarOrcamento();
        return salvarComAvisoDeStatus(os, anterior);
    }

    @Override
    @Transactional
    public OrdemServico aprovarOrcamento(Long id, String chave) {
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(id)
            .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));
        return aplicarDecisao(os, chave, os.getVersaoOrcamento(), true);
    }

    @Override
    @Transactional
    public OrdemServico decidirOrcamento(Long id, String chave, long versaoOrcamento, boolean aprovado) {
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResourceNotFoundException("OS não encontrada com ID: " + id));
        return aplicarDecisao(os, chave, versaoOrcamento, aprovado);
    }

    private OrdemServico aplicarDecisao(OrdemServico os, String chave, long versaoOrcamento, boolean aprovado) {
        StatusOS anterior = os.getStatus();
        if (!os.decidirOrcamento(chave, versaoOrcamento, aprovado)) {
            return os;
        }
        if (aprovado) {
            baixarEstoqueDosItens(os);
        }
        return salvarComAvisoDeStatus(os, anterior);
    }

    private OrdemServico salvarComAvisoDeStatus(OrdemServico os, StatusOS anterior) {
        OrdemServico salva = osRepositoryPort.salvar(os);
        if (salva.getStatus() != anterior) {
            clienteRepositoryPort.buscarPorId(salva.getClienteId())
                    .ifPresent(cliente -> notificarStatus(salva, cliente));
        }
        return salva;
    }

    private void notificarStatus(OrdemServico os, Cliente cliente) {
        String email = cliente.getEmail();
        if (email != null && !email.isBlank()) {
            notificationPort.notificar(new StatusOsNotification(os.getId(), os.getStatus(), email));
        }
    }

    @Override
    @Transactional
    public OrdemServico avaliarServico(Long id, String chave, int nota, String comentario) {
        OrdemServico os = osRepositoryPort.buscarPorIdParaAtualizacao(id)
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
