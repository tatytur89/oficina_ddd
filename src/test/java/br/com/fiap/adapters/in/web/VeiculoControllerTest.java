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
import br.com.fiap.domain.entities.Veiculo;
import br.com.fiap.ports.in.VeiculoUseCase;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = VeiculoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class VeiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VeiculoUseCase veiculoUseCase;

    private static final Veiculo VEICULO = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);

    @Test
    void deveCadastrarVeiculoComSucesso() throws Exception {
        when(veiculoUseCase.cadastrarVeiculo(any())).thenReturn(VEICULO);

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marca\":\"Toyota\",\"modelo\":\"Corolla\",\"ano\":2022,\"placa\":\"ABC1D23\",\"clienteId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dados.modelo").value("Corolla"));
    }

    @Test
    void deveRetornar404AoCadastrarComClienteInexistente() throws Exception {
        when(veiculoUseCase.cadastrarVeiculo(any()))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado com ID: 1"));

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marca\":\"Toyota\",\"modelo\":\"Corolla\",\"ano\":2022,\"placa\":\"ABC1D23\",\"clienteId\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar409AoCadastrarComPlacaDuplicada() throws Exception {
        when(veiculoUseCase.cadastrarVeiculo(any()))
                .thenThrow(new ResourceAlreadyExistsException("Veículo com placa informada já existe."));

        mockMvc.perform(post("/api/v1/veiculos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marca\":\"Toyota\",\"modelo\":\"Corolla\",\"ano\":2022,\"placa\":\"ABC1D23\",\"clienteId\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveListarVeiculos() throws Exception {
        when(veiculoUseCase.listarTodos()).thenReturn(List.of(VEICULO));

        mockMvc.perform(get("/api/v1/veiculos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].placa").value("ABC1D23"));
    }

    @Test
    void deveListarVeiculosPorCliente() throws Exception {
        when(veiculoUseCase.listarPorCliente(1L)).thenReturn(List.of(VEICULO));

        mockMvc.perform(get("/api/v1/veiculos/cliente/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar404AoListarPorClienteInexistente() throws Exception {
        when(veiculoUseCase.listarPorCliente(1L))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado com ID: 1"));

        mockMvc.perform(get("/api/v1/veiculos/cliente/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveBuscarVeiculoPorPlaca() throws Exception {
        when(veiculoUseCase.buscarPorPlaca("ABC1D23")).thenReturn(VEICULO);

        mockMvc.perform(get("/api/v1/veiculos/placa/ABC1D23"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar404AoBuscarPorPlacaInexistente() throws Exception {
        when(veiculoUseCase.buscarPorPlaca(anyString()))
                .thenThrow(new ResourceNotFoundException("Veículo não encontrado com a placa: ABC1D23"));

        mockMvc.perform(get("/api/v1/veiculos/placa/ABC1D23"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarVeiculoComSucesso() throws Exception {
        when(veiculoUseCase.atualizarVeiculo(any())).thenReturn(VEICULO);

        mockMvc.perform(put("/api/v1/veiculos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marca\":\"Toyota\",\"modelo\":\"Corolla\",\"ano\":2022,\"placa\":\"ABC1D23\",\"clienteId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveExcluirVeiculoComSucesso() throws Exception {
        mockMvc.perform(delete("/api/v1/veiculos/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar409AoExcluirVeiculoComOSVinculada() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceInUseException("Veículo possui ordens de serviço vinculadas e não pode ser excluído."))
                .when(veiculoUseCase).excluirVeiculo(anyLong());

        mockMvc.perform(delete("/api/v1/veiculos/1"))
                .andExpect(status().isConflict());
    }
}
