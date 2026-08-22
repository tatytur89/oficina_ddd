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
import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;
import br.com.fiap.ports.in.ServicoUseCase;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ServicoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class ServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicoUseCase servicoUseCase;

    private static final Servico SERVICO = new Servico(1L, "Troca de Óleo", "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);

    @Test
    void deveCadastrarServicoComSucesso() throws Exception {
        when(servicoUseCase.cadastrarServico(any())).thenReturn(SERVICO);

        mockMvc.perform(post("/api/v1/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Troca de Óleo\",\"descricao\":\"Descrição\",\"preco\":150.00,\"tipo\":\"MANUTENCAO\",\"tempoEstimadoMinutos\":60}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.nome").value("Troca de Óleo"));
    }

    @Test
    void deveRetornar400ParaTipoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Troca de Óleo\",\"descricao\":\"Descrição\",\"preco\":150.00,\"tipo\":\"INVALIDO\",\"tempoEstimadoMinutos\":60}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar409AoCadastrarComNomeDuplicado() throws Exception {
        when(servicoUseCase.cadastrarServico(any()))
                .thenThrow(new ResourceAlreadyExistsException("Serviço com nome informado já existe."));

        mockMvc.perform(post("/api/v1/servicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Troca de Óleo\",\"descricao\":\"Descrição\",\"preco\":150.00,\"tipo\":\"MANUTENCAO\",\"tempoEstimadoMinutos\":60}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveListarServicos() throws Exception {
        when(servicoUseCase.listarTodos()).thenReturn(List.of(SERVICO));

        mockMvc.perform(get("/api/v1/servicos"))
                .andExpect(status().isOk());
    }

    @Test
    void deveBuscarServicoPorId() throws Exception {
        when(servicoUseCase.buscarPorId(1L)).thenReturn(SERVICO);

        mockMvc.perform(get("/api/v1/servicos/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar404AoBuscarServicoInexistente() throws Exception {
        when(servicoUseCase.buscarPorId(99L))
                .thenThrow(new ResourceNotFoundException("Serviço não encontrado com ID: 99"));

        mockMvc.perform(get("/api/v1/servicos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarServicoComSucesso() throws Exception {
        when(servicoUseCase.atualizarServico(any())).thenReturn(SERVICO);

        mockMvc.perform(put("/api/v1/servicos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Troca de Óleo\",\"descricao\":\"Descrição\",\"preco\":150.00,\"tipo\":\"MANUTENCAO\",\"tempoEstimadoMinutos\":60}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveExcluirServicoComSucesso() throws Exception {
        mockMvc.perform(delete("/api/v1/servicos/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar404AoExcluirServicoInexistente() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Serviço não encontrado com ID: 1"))
                .when(servicoUseCase).excluirServico(anyLong());

        mockMvc.perform(delete("/api/v1/servicos/1"))
                .andExpect(status().isNotFound());
    }
}
