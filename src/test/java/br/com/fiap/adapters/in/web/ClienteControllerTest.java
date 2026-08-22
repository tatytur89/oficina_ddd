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
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.in.ClienteUseCase;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ClienteController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteUseCase clienteUseCase;

    private static final Cliente CLIENTE = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");

    @Test
    void deveCadastrarClienteComSucesso() throws Exception {
        when(clienteUseCase.cadastrarCliente(any())).thenReturn(CLIENTE);

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Robert\",\"documento\":\"12345678909\",\"email\":\"robert@email.com\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.dados.nome").value("Robert"));
    }

    @Test
    void deveRetornar400AoCadastrarComNomeVazio() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"documento\":\"12345678909\",\"email\":\"robert@email.com\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void deveRetornar409AoCadastrarComDocumentoDuplicado() throws Exception {
        when(clienteUseCase.cadastrarCliente(any()))
                .thenThrow(new ResourceAlreadyExistsException("Cliente com CPF/CNPJ informado já existe."));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Robert\",\"documento\":\"12345678909\",\"email\":\"robert@email.com\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveListarClientes() throws Exception {
        when(clienteUseCase.listarTodos()).thenReturn(List.of(CLIENTE));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].nome").value("Robert"));
    }

    @Test
    void deveBuscarClientePorDocumentoComSucesso() throws Exception {
        when(clienteUseCase.buscarPorDocumento("12345678909")).thenReturn(CLIENTE);

        mockMvc.perform(get("/api/v1/clientes/documento").param("documento", "12345678909"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.documento").value("12345678909"));
    }

    @Test
    void deveRetornar404AoBuscarClientePorDocumentoInexistente() throws Exception {
        when(clienteUseCase.buscarPorDocumento("00000000000"))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado"));

        mockMvc.perform(get("/api/v1/clientes/documento").param("documento", "00000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarClienteComSucesso() throws Exception {
        when(clienteUseCase.atualizarCliente(any())).thenReturn(CLIENTE);

        mockMvc.perform(put("/api/v1/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Robert\",\"documento\":\"12345678909\",\"email\":\"robert@email.com\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar404AoAtualizarClienteInexistente() throws Exception {
        when(clienteUseCase.atualizarCliente(any()))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado com ID: 1"));

        mockMvc.perform(put("/api/v1/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Robert\",\"documento\":\"12345678909\",\"email\":\"robert@email.com\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveExcluirClienteComSucesso() throws Exception {
        mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void deveRetornar404AoExcluirClienteInexistente() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Cliente não encontrado com ID: 1"))
                .when(clienteUseCase).excluirCliente(anyLong());

        mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isNotFound());
    }
}
