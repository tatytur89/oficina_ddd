package br.com.fiap.adapters.in.web;

import br.com.fiap.domain.entities.*;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.OrdemServicoUseCase;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.PecaRepositoryPort;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;
import br.com.fiap.ports.out.ServicoRepositoryPort;
import br.com.fiap.domain.valueobjects.TipoServico;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.notifications.email.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:decisao-integracao;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "api.security.token.secret=segredo-exclusivo-dos-testes-de-integracao",
        "app.admin.default-user=admin-integracao",
        "app.admin.default-password=senha-integracao"
})
@AutoConfigureMockMvc
class DecisaoOrcamentoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired OrdemServicoRepositoryPort ordens;
    @Autowired PecaRepositoryPort pecas;
    @Autowired OrdemServicoUseCase useCase;
    @Autowired TransactionTemplate tx;
    @Autowired ClienteRepositoryPort clientes;
    @Autowired VeiculoRepositoryPort veiculos;
    @Autowired ServicoRepositoryPort servicos;

    record Fixture(Long id, String chave, Long pecaId, Long segundaPecaId) {}
    record Estado(StatusOS status, long versao, Boolean decisao, int estoque, int segundoEstoque, int itens) {}

    private Fixture aguardando(boolean comPecaInsuficiente) {
        return tx.execute(status -> {
            var preco = new Preco(20);
            var peca = pecas.salvar(new Peca(null, "Filtro", "Filtro", UUID.randomUUID().toString(), preco, 5, 1));
            var segunda = comPecaInsuficiente
                    ? pecas.salvar(new Peca(null, "Pastilha", "Pastilha", UUID.randomUUID().toString(), preco, 0, 1)) : null;
            var itens = new java.util.ArrayList<PecaOS>();
            itens.add(new PecaOS(peca.getId(), peca.getNome(), peca.getCodigo(), 2, preco, new Preco(40)));
            if (segunda != null) itens.add(new PecaOS(segunda.getId(), segunda.getNome(), segunda.getCodigo(), 1, preco, preco));
            String chave = UUID.randomUUID().toString();
            var os = ordens.salvar(new OrdemServico(null, 1L, 2L, StatusOS.AGUARDANDO_APROVACAO,
                    LocalDateTime.now(), null, null, "obs", new Preco(100), new Preco(40), new Preco(140),
                    List.of(new ServicoOS(3L, "Revisao", 1, new Preco(100), new Preco(100))), itens,
                    chave, null, null, 3L, null));
            return new Fixture(os.getId(), chave, peca.getId(), segunda == null ? null : segunda.getId());
        });
    }

    @Test
    void duasOrdensConcorrentesNaoPodemConsumirAMesmaUltimaPeca() throws Exception {
        verificarEstoqueCompartilhado(3, false);
    }

    @Test
    void duasOrdensConcorrentesComSaldoDevemPreservarAmbasAsBaixas() throws Exception {
        verificarEstoqueCompartilhado(5, true);
    }

    private void verificarEstoqueCompartilhado(int saldoInicial, boolean ambasAprovadas) throws Exception {
        Fixture primeira = aguardando(false);
        Fixture segunda = tx.execute(status -> {
            var peca = pecas.buscarPorId(primeira.pecaId()).orElseThrow();
            pecas.salvar(new Peca(peca.getId(), peca.getNome(), peca.getDescricao(), peca.getCodigo(),
                    peca.getPreco(), saldoInicial, peca.getEstoqueMinimo()));
            String chave = UUID.randomUUID().toString();
            var os = ordens.salvar(new OrdemServico(null, 1L, 2L, StatusOS.AGUARDANDO_APROVACAO,
                    LocalDateTime.now(), null, null, "concorrencia", new Preco(100), new Preco(40), new Preco(140),
                    List.of(new ServicoOS(3L, "Revisao", 1, new Preco(100), new Preco(100))),
                    List.of(new PecaOS(peca.getId(), peca.getNome(), peca.getCodigo(), 2, peca.getPreco(), new Preco(40))),
                    chave, null, null, 3L, null));
            return new Fixture(os.getId(), chave, peca.getId(), null);
        });
        var primeiraBaixou = new CountDownLatch(1);
        var liberarCommit = new CountDownLatch(1);
        var segundaIniciou = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> tx.execute(status -> {
                useCase.decidirOrcamento(primeira.id(), primeira.chave(), 3L, true);
                primeiraBaixou.countDown();
                try {
                    if (!liberarCommit.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout do teste");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return true;
            }));
            try {
                assertTrue(primeiraBaixou.await(10, TimeUnit.SECONDS));
                var b = executor.submit(() -> {
                    segundaIniciou.countDown();
                    try {
                        useCase.decidirOrcamento(segunda.id(), segunda.chave(), 3L, true);
                        return true;
                    } catch (IllegalStateException e) {
                        assertTrue(e.getMessage().contains("Estoque insuficiente"));
                        return false;
                    }
                });
                assertTrue(segundaIniciou.await(5, TimeUnit.SECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class, () -> b.get(250, TimeUnit.MILLISECONDS));
                liberarCommit.countDown();
                assertTrue(a.get(10, TimeUnit.SECONDS));
                assertEquals(ambasAprovadas, b.get(10, TimeUnit.SECONDS));
                assertEquals(saldoInicial - (ambasAprovadas ? 4 : 2), estado(primeira).estoque());
                assertEquals(StatusOS.EM_EXECUCAO, estado(primeira).status());
                assertEquals(ambasAprovadas ? StatusOS.EM_EXECUCAO : StatusOS.AGUARDANDO_APROVACAO,
                        estado(segunda).status());
            } finally {
                liberarCommit.countDown();
            }
        }
    }

    private Estado estado(Fixture f) {
        return tx.execute(status -> {
            var os = ordens.buscarPorId(f.id()).orElseThrow();
            int estoque = pecas.buscarPorId(f.pecaId()).orElseThrow().getQuantidadeEstoque();
            int segundo = f.segundaPecaId() == null ? -1 : pecas.buscarPorId(f.segundaPecaId()).orElseThrow().getQuantidadeEstoque();
            return new Estado(os.getStatus(), os.getVersaoOrcamento(), os.getDecisaoOrcamento(), estoque, segundo, os.getPecas().size());
        });
    }

    private String decisao(Fixture f, long versao, boolean aprovado) {
        return "{\"chave\":\"" + f.chave() + "\",\"versaoOrcamento\":" + versao + ",\"aprovado\":" + aprovado + "}";
    }

    private String url(Fixture f) { return "/api/v1/ordens-servico/" + f.id() + "/decisao-orcamento"; }

    private String tokenAdmin() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"admin-integracao\",\"senha\":\"senha-integracao\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(login, "$.dados.token");
    }

    @Test
    void fluxoCompletoDaAberturaAEntregaPreservaHistoricoEOcultaFila() throws Exception {
        var ids = tx.execute(status -> {
            var cliente = clientes.salvar(new Cliente(null, "Cliente fluxo", "12345678909", "fluxo@example.com", "11999999999"));
            var veiculo = veiculos.salvar(new Veiculo(null, "Toyota", "Corolla", 2022, "ABC1D23", cliente.getId()));
            var servico = servicos.salvar(new Servico(null, "Revisao completa", "Revisao", new Preco(100), TipoServico.REVISAO, 60));
            var peca = pecas.salvar(new Peca(null, "Filtro fluxo", "Filtro", UUID.randomUUID().toString(), new Preco(25), 5, 1));
            return List.of(cliente.getId(), veiculo.getId(), servico.getId(), peca.getId());
        });
        String bearer = "Bearer " + tokenAdmin();
        String abertura = mvc.perform(post("/api/v1/ordens-servico").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"clienteId":%d,"veiculoId":%d,
                                 "servicos":[{"servicoId":%d,"quantidade":1}],
                                 "pecas":[{"pecaId":%d,"quantidade":2}]}
                                """.formatted(ids.get(0), ids.get(1), ids.get(2), ids.get(3))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.dados.valorTotal").value(150))
                .andReturn().getResponse().getContentAsString();
        Number idCriado = JsonPath.read(abertura, "$.dados.id");
        String chave = JsonPath.read(abertura, "$.dados.chaveAcesso");
        long id = idCriado.longValue();
        String base = "/api/v1/ordens-servico/" + id;
        mvc.perform(patch(base + "/status/EM_DIAGNOSTICO").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(patch(base + "/orcamento").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.versaoOrcamento").value(1));
        String decisao = "{\"chave\":\"" + chave + "\",\"versaoOrcamento\":1,\"aprovado\":true}";
        mvc.perform(post(base + "/decisao-orcamento").contentType(MediaType.APPLICATION_JSON).content(decisao)).andExpect(status().isOk());
        mvc.perform(post(base + "/decisao-orcamento").contentType(MediaType.APPLICATION_JSON).content(decisao)).andExpect(status().isOk());
        mvc.perform(patch(base + "/status/FINALIZADA").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(patch(base + "/status/ENTREGUE").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/ordens-servico").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[?(@.id == " + id + ")]").isEmpty());
        mvc.perform(get(base).header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.status").value("ENTREGUE"));
        int estoqueFinal = tx.execute(status -> pecas.buscarPorId(ids.get(3)).orElseThrow().getQuantidadeEstoque());
        assertEquals(3, estoqueFinal);
    }

    @Test
    void callbackPublicoAutenticaPelaChaveEPersisteAprovacaoComEstoque() throws Exception {
        var f = aguardando(false);
        mvc.perform(post(url(f)).contentType(MediaType.APPLICATION_JSON).content(decisao(f, 3, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.dados.versaoOrcamento").value(3))
                .andExpect(jsonPath("$.dados.decisaoOrcamento").value(true));
        assertEquals(new Estado(StatusOS.EM_EXECUCAO, 3, true, 3, -1, 1), estado(f));
    }

    @Test
    void recusaPublicaMantemItensEEstoque() throws Exception {
        var f = aguardando(false);
        mvc.perform(post(url(f)).contentType(MediaType.APPLICATION_JSON).content(decisao(f, 3, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.status").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$.dados.decisaoOrcamento").value(false));
        assertEquals(new Estado(StatusOS.EM_DIAGNOSTICO, 3, false, 5, -1, 1), estado(f));
    }

    @Test
    void callbackRepetidoNaoConsomeEstoqueNovamenteAposRecarregarDoBanco() throws Exception {
        var f = aguardando(false);
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mvc.perform(post(url(f)).contentType(MediaType.APPLICATION_JSON).content(decisao(f, 3, true)))
                    .andExpect(status().isOk());
        }
        assertEquals(new Estado(StatusOS.EM_EXECUCAO, 3, true, 3, -1, 1), estado(f));
    }

    @Test
    void duasAprovacoesSimultaneasDaMesmaOSConsomemEstoqueUmaUnicaVez() throws Exception {
        var f = aguardando(false);
        var prontas = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<StatusOS> aprovar = () -> {
                prontas.countDown();
                if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Inicio da disputa expirou");
                return useCase.decidirOrcamento(f.id(), f.chave(), 3L, true).getStatus();
            };
            var primeira = executor.submit(aprovar);
            var segunda = executor.submit(aprovar);
            try {
                assertTrue(prontas.await(5, TimeUnit.SECONDS));
            } finally {
                iniciar.countDown();
            }
            assertEquals(StatusOS.EM_EXECUCAO, primeira.get(10, TimeUnit.SECONDS));
            assertEquals(StatusOS.EM_EXECUCAO, segunda.get(10, TimeUnit.SECONDS));
        }
        assertEquals(new Estado(StatusOS.EM_EXECUCAO, 3, true, 3, -1, 1), estado(f));
    }

    @Test
    void callbackComChaveErradaNaoAlteraOSOuEstoque() throws Exception {
        var f = aguardando(false);
        mvc.perform(post(url(f)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chave\":\"errada\",\"versaoOrcamento\":3,\"aprovado\":true}"))
                .andExpect(status().isBadRequest());
        assertEquals(new Estado(StatusOS.AGUARDANDO_APROVACAO, 3, null, 5, -1, 1), estado(f));
    }

    @Test
    void callbackAntigoNaoAprovaNovoOrcamentoDepoisDeRecusa() throws Exception {
        var f = aguardando(false);
        mvc.perform(post(url(f)).contentType(MediaType.APPLICATION_JSON).content(decisao(f, 3, false))).andExpect(status().isOk());
        useCase.enviarOrcamento(f.id());
        mvc.perform(post(url(f)).contentType(MediaType.APPLICATION_JSON).content(decisao(f, 3, true))).andExpect(status().isBadRequest());
        assertEquals(new Estado(StatusOS.AGUARDANDO_APROVACAO, 4, null, 5, -1, 1), estado(f));
    }

    @Test
    void falhaEmUmaPecaFazRollbackDaBaixaAnteriorEDaDecisao() throws Exception {
        var f = aguardando(true);
        mvc.perform(post(url(f)).contentType(MediaType.APPLICATION_JSON).content(decisao(f, 3, true))).andExpect(status().isBadRequest());
        assertEquals(new Estado(StatusOS.AGUARDANDO_APROVACAO, 3, null, 5, 0, 2), estado(f));
    }

    @Test
    void rotasAdministrativasContinuamProtegidasSemToken() throws Exception {
        mvc.perform(get("/api/v1/ordens-servico")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/ordens-servico").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":1,\"veiculoId\":2}")).andExpect(status().isForbidden());
    }

    @Test
    void loginPublicoEmiteTokenMasAdminNaoPodeContornarAprovacao() throws Exception {
        String token = tokenAdmin();
        var f = aguardando(false);
        mvc.perform(get("/api/v1/ordens-servico").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/ordens-servico/" + f.id() + "/status/EM_EXECUCAO")
                        .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        assertEquals(new Estado(StatusOS.AGUARDANDO_APROVACAO, 3, null, 5, -1, 1), estado(f));
    }

    static Stream<String> payloadsInvalidos() {
        return Stream.of("{}", "{\"chave\":\" \",\"versaoOrcamento\":0,\"aprovado\":true}",
                "{\"chave\":\"chave\",\"aprovado\":true}", "{\"chave\":\"chave\",\"versaoOrcamento\":0}",
                "{\"chave\":\"chave\",\"versaoOrcamento\":-1,\"aprovado\":false}");
    }

    @ParameterizedTest
    @MethodSource("payloadsInvalidos")
    void rejeitaPayloadIncompletoOuInvalidoSemExigirBearer(String payload) throws Exception {
        mvc.perform(post("/api/v1/ordens-servico/999/decisao-orcamento").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }
}
