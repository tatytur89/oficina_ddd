package br.com.fiap.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.com.fiap.adapters.out.security.SecurityFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InfoController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"app.build.commit=abc1234", "HOSTNAME=oficina-api-pod-1"})
class InfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornarCommitEPodEmExecucao() throws Exception {
        mockMvc.perform(get("/api/v1/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.aplicacao").value("oficina-ddd"))
                .andExpect(jsonPath("$.dados.commit").value("abc1234"))
                .andExpect(jsonPath("$.dados.pod").value("oficina-api-pod-1"));
    }
}
