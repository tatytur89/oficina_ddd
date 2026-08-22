package br.com.fiap.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;

public class OrdemServico {
    private Long id;
    private Long clienteId;
    private Long veiculoId;
    private StatusOS status;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataPrevistaEntrega;
    private LocalDateTime dataConclusao;
    private String observacoes;
    private Preco valorServicos;
    private Preco valorPecas;
    private Preco valorTotal;
    private List<ServicoOS> servicos;
    private List<PecaOS> pecas;

    public OrdemServico(Long id, Long clienteId, Long veiculoId, StatusOS status,
                        LocalDateTime dataAbertura, LocalDateTime dataPrevistaEntrega,
                        LocalDateTime dataConclusao, String observacoes,
                        Preco valorServicos, Preco valorPecas, Preco valorTotal,
                        List<ServicoOS> servicos, List<PecaOS> pecas) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.status = status;
        this.dataAbertura = dataAbertura;
        this.dataPrevistaEntrega = dataPrevistaEntrega;
        this.dataConclusao = dataConclusao;
        this.observacoes = observacoes;
        this.valorServicos = valorServicos;
        this.valorPecas = valorPecas;
        this.valorTotal = valorTotal;
        this.servicos = servicos != null ? servicos : new ArrayList<>();
        this.pecas = pecas != null ? pecas : new ArrayList<>();
    }

    public void adicionarServico(Servico servico, int quantidade) {
        if (status != StatusOS.RECEBIDA && status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Não é possível adicionar serviços neste status.");
        }
        Preco valorItem = servico.getPreco().multiplicar(quantidade);
        this.servicos.add(new ServicoOS(servico.getId(), servico.getNome(), quantidade, servico.getPreco(), valorItem));
        recalcularValores();
    }

    public void adicionarPeca(Peca peca, int quantidade) {
        if (status != StatusOS.RECEBIDA && status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Não é possível adicionar peças neste status.");
        }
        Preco valorItem = peca.getPreco().multiplicar(quantidade);
        this.pecas.add(new PecaOS(peca.getId(), peca.getNome(), peca.getCodigo(), quantidade, peca.getPreco(), valorItem));
        recalcularValores();
    }

    public void enviarOrcamento() {
        if (status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("A OS deve estar Em Diagnóstico para enviar orçamento.");
        }
        if (servicos.isEmpty()) {
            throw new IllegalStateException("A OS deve ter pelo menos um serviço para enviar orçamento.");
        }
        this.status = StatusOS.AGUARDANDO_APROVACAO;
    }

    public void transicionarPara(StatusOS novoStatus) {
        if (!status.podeTransicionarPara(novoStatus)) {
            throw new IllegalStateException(
                String.format("Transição inválida: %s → %s. Próximos status válidos: %s",
                    status.getDescricao(), novoStatus.getDescricao(), status.proximosStatusValidos())
            );
        }
        this.status = novoStatus;
        if (novoStatus == StatusOS.FINALIZADA) {
            this.dataConclusao = LocalDateTime.now();
        }
    }

    private void recalcularValores() {
        this.valorServicos = servicos.stream()
            .map(ServicoOS::getValorTotal)
            .reduce(Preco::somar)
            .orElse(new Preco(BigDecimal.ZERO));
        
        this.valorPecas = pecas.stream()
            .map(PecaOS::getValorTotal)
            .reduce(Preco::somar)
            .orElse(new Preco(BigDecimal.ZERO));
        
        this.valorTotal = this.valorServicos.somar(this.valorPecas);
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public Long getVeiculoId() { return veiculoId; }
    public StatusOS getStatus() { return status; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public LocalDateTime getDataPrevistaEntrega() { return dataPrevistaEntrega; }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public String getObservacoes() { return observacoes; }
    public Preco getValorServicos() { return valorServicos; }
    public Preco getValorPecas() { return valorPecas; }
    public Preco getValorTotal() { return valorTotal; }
    public List<ServicoOS> getServicos() { return servicos; }
    public List<PecaOS> getPecas() { return pecas; }
}
