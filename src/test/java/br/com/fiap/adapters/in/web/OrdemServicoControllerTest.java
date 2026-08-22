package br.com.fiap.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.adapters.out.security.SecurityFilter;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.OrdemServicoUseCase;
import br.com.fiap.ports.in.TempoMedioExecucao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = OrdemServicoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class OrdemServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdemServicoUseCase osUseCase;

    private static final OrdemServico OS = new OrdemServico(1L, 1L, 1L, StatusOS.RECEBIDA, LocalDateTime.now(), null, null, "obs",
            new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), null, null, null, null, null);

    @Test
    void deveCriarOSComSucesso() throws Exception {
        when(osUseCase.criarOS(any())).thenReturn(OS);

        mockMvc.perform(post("/api/v1/ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"veiculoId\":1,\"observacoes\":\"obs\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.status").value("RECEBIDA"));
    }

    @Test
    void deveRetornar404AoCriarOSComClienteInexistente() throws Exception {
        when(osUseCase.criarOS(any()))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado com ID: 1"));

        mockMvc.perform(post("/api/v1/ordens-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"veiculoId\":1,\"observacoes\":\"obs\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRealizarDiagnosticoComSucesso() throws Exception {
        when(osUseCase.realizarDiagnostico(eq(1L), any(), any(), any())).thenReturn(OS);

        mockMvc.perform(post("/api/v1/ordens-servico/1/diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicos\":[{\"servicoId\":1,\"quantidade\":1}],\"pecas\":[{\"pecaId\":1,\"quantidade\":2}]}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar400AoDiagnosticarForaDeRecebida() throws Exception {
        when(osUseCase.realizarDiagnostico(eq(1L), any(), any(), any()))
                .thenThrow(new IllegalStateException("Transição inválida"));

        mockMvc.perform(post("/api/v1/ordens-servico/1/diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAdicionarServicoComSucesso() throws Exception {
        when(osUseCase.adicionarServico(1L, 1L, 1)).thenReturn(OS);

        mockMvc.perform(post("/api/v1/ordens-servico/1/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicoId\":1,\"quantidade\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar400AoAdicionarServicoForaDeDiagnostico() throws Exception {
        when(osUseCase.adicionarServico(anyLong(), anyLong(), anyInt()))
                .thenThrow(new IllegalStateException("Não é possível adicionar serviços neste status."));

        mockMvc.perform(post("/api/v1/ordens-servico/1/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicoId\":1,\"quantidade\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAdicionarPecaComSucesso() throws Exception {
        when(osUseCase.adicionarPeca(1L, 1L, 2)).thenReturn(OS);

        mockMvc.perform(post("/api/v1/ordens-servico/1/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pecaId\":1,\"quantidade\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar404AoAdicionarPecaInexistente() throws Exception {
        when(osUseCase.adicionarPeca(anyLong(), anyLong(), anyInt()))
                .thenThrow(new ResourceNotFoundException("Peça não encontrada com ID: 1"));

        mockMvc.perform(post("/api/v1/ordens-servico/1/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pecaId\":1,\"quantidade\":2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarTodasAsOS() throws Exception {
        when(osUseCase.listarTodas()).thenReturn(List.of(OS));

        mockMvc.perform(get("/api/v1/ordens-servico"))
                .andExpect(status().isOk());
    }

    @Test
    void deveBuscarOSPorId() throws Exception {
        when(osUseCase.buscarPorId(1L)).thenReturn(OS);

        mockMvc.perform(get("/api/v1/ordens-servico/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarOSPorCliente() throws Exception {
        when(osUseCase.listarPorCliente(1L)).thenReturn(List.of(OS));

        mockMvc.perform(get("/api/v1/ordens-servico/cliente/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarOSPorStatus() throws Exception {
        when(osUseCase.listarPorStatus(StatusOS.RECEBIDA)).thenReturn(List.of(OS));

        mockMvc.perform(get("/api/v1/ordens-servico/status/RECEBIDA"))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarOSPorPeriodo() throws Exception {
        when(osUseCase.listarPorPeriodo(any(), any())).thenReturn(List.of(OS));

        mockMvc.perform(get("/api/v1/ordens-servico/periodo")
                        .param("inicio", "2026-08-01T00:00:00")
                        .param("fim", "2026-08-31T23:59:59"))
                .andExpect(status().isOk());
    }

    @Test
    void deveEnviarOrcamentoComSucesso() throws Exception {
        when(osUseCase.enviarOrcamento(1L)).thenReturn(OS);

        mockMvc.perform(patch("/api/v1/ordens-servico/1/orcamento"))
                .andExpect(status().isOk());
    }

    @Test
    void deveAtualizarStatusComSucesso() throws Exception {
        when(osUseCase.atualizarStatus(1L, StatusOS.EM_DIAGNOSTICO)).thenReturn(OS);

        mockMvc.perform(patch("/api/v1/ordens-servico/1/status/EM_DIAGNOSTICO"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar400ParaTransicaoInvalida() throws Exception {
        when(osUseCase.atualizarStatus(anyLong(), any()))
                .thenThrow(new IllegalStateException("Transição inválida"));

        mockMvc.perform(patch("/api/v1/ordens-servico/1/status/ENTREGUE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarTempoMedioDeExecucao() throws Exception {
        when(osUseCase.calcularTempoMedioExecucao()).thenReturn(new TempoMedioExecucao(120.5, 3));

        mockMvc.perform(get("/api/v1/ordens-servico/metricas/tempo-medio-execucao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.quantidadeOSConsideradas").value(3));
    }
}
