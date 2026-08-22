package br.com.fiap.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.adapters.out.security.SecurityFilter;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.OrdemServicoUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AcompanhamentoOSController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class AcompanhamentoOSControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdemServicoUseCase osUseCase;

    private OrdemServico os(StatusOS status) {
        return new OrdemServico(1L, 1L, 1L, status, LocalDateTime.now(), null, null, "obs",
                new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO), new Preco(BigDecimal.ZERO),
                null, null, "chave-correta", null, null);
    }

    @Test
    void deveExibirPaginaComBotaoQuandoAguardandoAprovacao() throws Exception {
        when(osUseCase.buscarParaAcompanhamento(1L, "chave-correta")).thenReturn(os(StatusOS.AGUARDANDO_APROVACAO));

        mockMvc.perform(get("/acompanhamento/1").param("chave", "chave-correta"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Aprovar orçamento")));
    }

    @Test
    void naoDeveExibirBotaoForaDeAguardandoAprovacao() throws Exception {
        when(osUseCase.buscarParaAcompanhamento(1L, "chave-correta")).thenReturn(os(StatusOS.EM_EXECUCAO));

        mockMvc.perform(get("/acompanhamento/1").param("chave", "chave-correta"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Aprovar orçamento"))));
    }

    @Test
    void deveRetornar400ParaChaveInvalida() throws Exception {
        when(osUseCase.buscarParaAcompanhamento(anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("Chave de acesso inválida."));

        mockMvc.perform(get("/acompanhamento/1").param("chave", "errada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar404ParaOSInexistente() throws Exception {
        when(osUseCase.buscarParaAcompanhamento(anyLong(), anyString()))
                .thenThrow(new ResourceNotFoundException("OS não encontrada com ID: 1"));

        mockMvc.perform(get("/acompanhamento/1").param("chave", "chave-correta"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRedirecionarAposAprovarComSucesso() throws Exception {
        when(osUseCase.aprovarOrcamento(1L, "chave-correta")).thenReturn(os(StatusOS.EM_EXECUCAO));

        mockMvc.perform(post("/acompanhamento/1/aprovar").param("chave", "chave-correta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acompanhamento/1?chave=chave-correta"));
    }

    @Test
    void deveRetornar400AoAprovarComChaveInvalida() throws Exception {
        when(osUseCase.aprovarOrcamento(anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("Chave de acesso inválida."));

        mockMvc.perform(post("/acompanhamento/1/aprovar").param("chave", "errada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveExibirFormularioDeAvaliacaoQuandoEntregueENaoAvaliada() throws Exception {
        when(osUseCase.buscarParaAcompanhamento(1L, "chave-correta")).thenReturn(os(StatusOS.ENTREGUE));

        mockMvc.perform(get("/acompanhamento/1").param("chave", "chave-correta"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Enviar avaliação")));
    }

    @Test
    void deveRedirecionarAposAvaliarComSucesso() throws Exception {
        when(osUseCase.avaliarServico(1L, "chave-correta", 5, "Ótimo")).thenReturn(os(StatusOS.ENTREGUE));

        mockMvc.perform(post("/acompanhamento/1/avaliar")
                        .param("chave", "chave-correta")
                        .param("nota", "5")
                        .param("comentario", "Ótimo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/acompanhamento/1?chave=chave-correta"));
    }

    @Test
    void deveRetornar400AoAvaliarDuasVezes() throws Exception {
        when(osUseCase.avaliarServico(anyLong(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenThrow(new IllegalStateException("Esta OS já foi avaliada."));

        mockMvc.perform(post("/acompanhamento/1/avaliar")
                        .param("chave", "chave-correta")
                        .param("nota", "5"))
                .andExpect(status().isBadRequest());
    }
}
