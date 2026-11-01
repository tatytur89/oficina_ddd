package br.com.fiap.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private String chaveAcesso;
    private Integer notaAvaliacao;
    private String comentarioAvaliacao;
    private long versaoOrcamento;
    private Boolean decisaoOrcamento;

    public OrdemServico(Long id, Long clienteId, Long veiculoId, StatusOS status,
                        LocalDateTime dataAbertura, LocalDateTime dataPrevistaEntrega,
                        LocalDateTime dataConclusao, String observacoes,
                        Preco valorServicos, Preco valorPecas, Preco valorTotal,
                        List<ServicoOS> servicos, List<PecaOS> pecas, String chaveAcesso,
                        Integer notaAvaliacao, String comentarioAvaliacao) {
        this(id, clienteId, veiculoId, status, dataAbertura, dataPrevistaEntrega, dataConclusao,
                observacoes, valorServicos, valorPecas, valorTotal, servicos, pecas, chaveAcesso,
                notaAvaliacao, comentarioAvaliacao, 0, null);
    }

    public OrdemServico(Long id, Long clienteId, Long veiculoId, StatusOS status,
                        LocalDateTime dataAbertura, LocalDateTime dataPrevistaEntrega,
                        LocalDateTime dataConclusao, String observacoes,
                        Preco valorServicos, Preco valorPecas, Preco valorTotal,
                        List<ServicoOS> servicos, List<PecaOS> pecas, String chaveAcesso,
                        Integer notaAvaliacao, String comentarioAvaliacao,
                        long versaoOrcamento, Boolean decisaoOrcamento) {
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
        this.servicos = servicos != null ? new ArrayList<>(servicos) : new ArrayList<>();
        this.pecas = pecas != null ? new ArrayList<>(pecas) : new ArrayList<>();
        this.chaveAcesso = chaveAcesso;
        this.notaAvaliacao = notaAvaliacao;
        this.comentarioAvaliacao = comentarioAvaliacao;
        this.versaoOrcamento = versaoOrcamento;
        this.decisaoOrcamento = decisaoOrcamento;
    }

    public static OrdemServico abrir(Long clienteId, Long veiculoId, String observacoes) {
        if (clienteId == null || clienteId <= 0 || veiculoId == null || veiculoId <= 0) {
            throw new IllegalArgumentException("Cliente e veículo devem ter IDs positivos.");
        }
        Preco zero = new Preco(BigDecimal.ZERO);
        return new OrdemServico(null, clienteId, veiculoId, StatusOS.RECEBIDA, LocalDateTime.now(),
                null, null, observacoes, zero, zero, zero, List.of(), List.of(),
                UUID.randomUUID().toString(), null, null);
    }

    public void registrarServicoSolicitado(Servico servico, int quantidade) {
        validarAbertura();
        incluirServico(servico, quantidade);
    }

    public void registrarPecaSolicitada(Peca peca, int quantidade) {
        validarAbertura();
        incluirPeca(peca, quantidade);
    }

    private void validarAbertura() {
        if (id != null || status != StatusOS.RECEBIDA) {
            throw new IllegalStateException("Itens solicitados só podem ser registrados na abertura da OS.");
        }
    }

    public void definirDataPrevistaEntrega(LocalDateTime dataPrevistaEntrega) {
        this.dataPrevistaEntrega = dataPrevistaEntrega;
    }

    public void adicionarServico(Servico servico, int quantidade) {
        if (status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Não é possível adicionar serviços neste status.");
        }
        incluirServico(servico, quantidade);
    }

    private void incluirServico(Servico servico, int quantidade) {
        validarQuantidade(quantidade);
        Preco valorItem = servico.getPreco().multiplicar(quantidade);
        this.servicos.add(new ServicoOS(servico.getId(), servico.getNome(), quantidade, servico.getPreco(), valorItem));
        recalcularValores();
    }

    public void adicionarPeca(Peca peca, int quantidade) {
        if (status != StatusOS.EM_DIAGNOSTICO) {
            throw new IllegalStateException("Não é possível adicionar peças neste status.");
        }
        incluirPeca(peca, quantidade);
    }

    private void incluirPeca(Peca peca, int quantidade) {
        validarQuantidade(quantidade);
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
        this.versaoOrcamento++;
        this.decisaoOrcamento = null;
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
    }

    public void validarChaveAcesso(String chave) {
        if (chave == null || !chave.equals(chaveAcesso)) {
            throw new IllegalArgumentException("Chave de acesso inválida.");
        }
    }

    public void aprovarOrcamento(String chave) {
        decidirOrcamento(chave, versaoOrcamento, true);
    }

    public boolean decidirOrcamento(String chave, long versao, boolean aprovado) {
        validarChaveAcesso(chave);
        if (versao < 0 || versao != versaoOrcamento) {
            throw new IllegalStateException("A decisão se refere a uma versão desatualizada do orçamento.");
        }
        if (decisaoOrcamento != null) {
            if (decisaoOrcamento == aprovado) {
                return false;
            }
            throw new IllegalStateException("Este orçamento já recebeu uma decisão diferente.");
        }
        if (status != StatusOS.AGUARDANDO_APROVACAO) {
            throw new IllegalStateException("A OS deve estar Aguardando aprovação para receber uma decisão.");
        }
        if (aprovado) {
            aplicarTransicao(StatusOS.EM_EXECUCAO);
        } else {
            this.status = StatusOS.EM_DIAGNOSTICO;
        }
        this.decisaoOrcamento = aprovado;
        return true;
    }

    public void avaliarServico(int nota, String comentario) {
        if (status != StatusOS.ENTREGUE) {
            throw new IllegalStateException("A OS deve estar Entregue para ser avaliada.");
        }
        if (notaAvaliacao != null) {
            throw new IllegalStateException("Esta OS já foi avaliada.");
        }
        if (nota < 1 || nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 1 e 5.");
        }
        this.notaAvaliacao = nota;
        this.comentarioAvaliacao = comentario;
    }

    public void transicionarPara(StatusOS novoStatus) {
        if (novoStatus == StatusOS.EM_EXECUCAO) {
            throw new IllegalStateException("O início da execução exige a aprovação do orçamento pelo cliente.");
        }
        if (novoStatus == StatusOS.AGUARDANDO_APROVACAO) {
            enviarOrcamento();
            return;
        }
        aplicarTransicao(novoStatus);
    }

    private void aplicarTransicao(StatusOS novoStatus) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("O novo status é obrigatório.");
        }
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
    public String getChaveAcesso() { return chaveAcesso; }
    public Integer getNotaAvaliacao() { return notaAvaliacao; }
    public String getComentarioAvaliacao() { return comentarioAvaliacao; }
    public long getVersaoOrcamento() { return versaoOrcamento; }
    public Boolean getDecisaoOrcamento() { return decisaoOrcamento; }
}
