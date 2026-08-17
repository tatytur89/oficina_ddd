package br.com.fiap.application.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.OrdemServicoUseCase;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;

@Service
public class OrdemServicoService implements OrdemServicoUseCase {

    private final OrdemServicoRepositoryPort osRepositoryPort;

    public OrdemServicoService(OrdemServicoRepositoryPort osRepositoryPort) {
        this.osRepositoryPort = osRepositoryPort;
    }

    @Override
    public OrdemServico criarOS(OrdemServico os) {
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
        return osRepositoryPort.salvar(novaOS);
    }

    @Override
    public Optional<OrdemServico> buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da OS não pode ser nulo.");
        }
        return osRepositoryPort.buscarPorId(id);
    }

    @Override
    public List<OrdemServico> listarTodas() {
        return osRepositoryPort.buscarTodas();
    }

    @Override
    public List<OrdemServico> listarPorCliente(Long clienteId) {
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
            .orElseThrow(() -> new IllegalArgumentException("OS não encontrada com ID: " + id));
        
        os.transicionarPara(novoStatus);
        return osRepositoryPort.salvar(os);
    }

    @Override
    @Transactional
    public OrdemServico enviarOrcamento(Long id) {
        OrdemServico os = osRepositoryPort.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("OS não encontrada com ID: " + id));
        
        os.enviarOrcamento();
        return osRepositoryPort.salvar(os);
    }
}
