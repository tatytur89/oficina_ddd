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
import br.com.fiap.application.exceptions.ResourceInUseException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.ports.in.PecaUseCase;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PecaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class PecaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PecaUseCase pecaUseCase;

    private static final Peca PECA = new Peca(1L, "Filtro de Óleo", "Descrição", "FIL001", new Preco(BigDecimal.valueOf(45.90)), 50, 10);

    @Test
    void deveCadastrarPecaComSucesso() throws Exception {
        when(pecaUseCase.cadastrarPeca(any())).thenReturn(PECA);

        mockMvc.perform(post("/api/v1/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Filtro de Óleo\",\"descricao\":\"Descrição\",\"codigo\":\"FIL001\",\"preco\":45.90,\"quantidadeEstoque\":50,\"estoqueMinimo\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.codigo").value("FIL001"));
    }

    @Test
    void deveRetornar409AoCadastrarComCodigoDuplicado() throws Exception {
        when(pecaUseCase.cadastrarPeca(any()))
                .thenThrow(new ResourceAlreadyExistsException("Peça com código informado já existe."));

        mockMvc.perform(post("/api/v1/pecas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Filtro de Óleo\",\"descricao\":\"Descrição\",\"codigo\":\"FIL001\",\"preco\":45.90,\"quantidadeEstoque\":50,\"estoqueMinimo\":10}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveListarPecas() throws Exception {
        when(pecaUseCase.listarTodas()).thenReturn(List.of(PECA));

        mockMvc.perform(get("/api/v1/pecas"))
                .andExpect(status().isOk());
    }

    @Test
    void deveBuscarPecaPorId() throws Exception {
        when(pecaUseCase.buscarPorId(1L)).thenReturn(PECA);

        mockMvc.perform(get("/api/v1/pecas/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveBuscarPecaPorCodigo() throws Exception {
        when(pecaUseCase.buscarPorCodigo("FIL001")).thenReturn(PECA);

        mockMvc.perform(get("/api/v1/pecas/codigo/FIL001"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar404AoBuscarPorCodigoInexistente() throws Exception {
        when(pecaUseCase.buscarPorCodigo("FIL999"))
                .thenThrow(new ResourceNotFoundException("Peça não encontrada com código: FIL999"));

        mockMvc.perform(get("/api/v1/pecas/codigo/FIL999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarPecaComSucesso() throws Exception {
        when(pecaUseCase.atualizarPeca(any())).thenReturn(PECA);

        mockMvc.perform(put("/api/v1/pecas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Filtro de Óleo\",\"descricao\":\"Descrição\",\"codigo\":\"FIL001\",\"preco\":45.90,\"estoqueMinimo\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveExcluirPecaComSucesso() throws Exception {
        mockMvc.perform(delete("/api/v1/pecas/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar409AoExcluirPecaComOSPendente() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceInUseException("Peça está vinculada a ordens de serviço pendentes de execução e não pode ser excluída."))
                .when(pecaUseCase).excluirPeca(anyLong());

        mockMvc.perform(delete("/api/v1/pecas/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveReporEstoqueComSucesso() throws Exception {
        mockMvc.perform(put("/api/v1/pecas/1/repor-estoque")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":10}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(pecaUseCase).reporEstoque(1L, 10);
    }

    @Test
    void deveRetornar400AoReporEstoqueComQuantidadeInvalida() throws Exception {
        mockMvc.perform(put("/api/v1/pecas/1/repor-estoque")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantidade\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarEstoqueBaixo() throws Exception {
        when(pecaUseCase.listarEstoqueBaixo()).thenReturn(List.of(PECA));

        mockMvc.perform(get("/api/v1/pecas/estoque-baixo"))
                .andExpect(status().isOk());
    }
}
