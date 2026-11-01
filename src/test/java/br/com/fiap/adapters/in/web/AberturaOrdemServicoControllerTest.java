package br.com.fiap.adapters.in.web;

import br.com.fiap.adapters.out.security.SecurityFilter;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.CriarOrdemServicoCommand;
import br.com.fiap.ports.in.ItemDiagnostico;
import br.com.fiap.ports.in.OrdemServicoUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrdemServicoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AberturaOrdemServicoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrdemServicoUseCase useCase;

    @Test
    void encaminhaItensEQuantidadesERetornaIdentificacao() throws Exception {
        Preco zero = new Preco(BigDecimal.ZERO);
        when(useCase.criarOS(any(CriarOrdemServicoCommand.class))).thenReturn(new OrdemServico(42L, 1L, 2L,
                StatusOS.RECEBIDA, LocalDateTime.now(), null, null, "Freio", zero, zero, zero, null, null, "chave", null, null));

        mvc.perform(post("/api/v1/ordens-servico").contentType(MediaType.APPLICATION_JSON).content("""
                {"clienteId":1,"veiculoId":2,"observacoes":"Freio",
                 "servicos":[{"servicoId":3,"quantidade":2}],
                 "pecas":[{"pecaId":4,"quantidade":3}]}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.id").value(42))
                .andExpect(jsonPath("$.dados.status").value("RECEBIDA"));

        var captor = ArgumentCaptor.forClass(CriarOrdemServicoCommand.class);
        verify(useCase).criarOS(captor.capture());
        assertEquals(1L, captor.getValue().clienteId());
        assertEquals(2L, captor.getValue().veiculoId());
        assertEquals("Freio", captor.getValue().observacoes());
        assertEquals(new ItemDiagnostico(3L, 2), captor.getValue().servicos().getFirst());
        assertEquals(new ItemDiagnostico(4L, 3), captor.getValue().pecas().getFirst());
    }

    static Stream<String> dadosInvalidos() {
        return Stream.of(
                "{\"clienteId\":0,\"veiculoId\":2}",
                "{\"clienteId\":1,\"veiculoId\":-2}",
                "{\"clienteId\":1,\"veiculoId\":2,\"servicos\":[null]}",
                "{\"clienteId\":1,\"veiculoId\":2,\"pecas\":[null]}",
                "{\"clienteId\":1,\"veiculoId\":2,\"servicos\":[{\"servicoId\":3,\"quantidade\":0}]}",
                "{\"clienteId\":1,\"veiculoId\":2,\"pecas\":[{\"pecaId\":4,\"quantidade\":-1}]}",
                "{\"clienteId\":1,\"veiculoId\":2,\"servicos\":[{\"quantidade\":1}]}",
                "{\"clienteId\":1,\"veiculoId\":2,\"pecas\":[{\"pecaId\":4}]}"
        );
    }

    @ParameterizedTest
    @MethodSource("dadosInvalidos")
    void rejeitaDadosInvalidosAntesDeExecutarCasoDeUso(String body) throws Exception {
        mvc.perform(post("/api/v1/ordens-servico").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }
}
