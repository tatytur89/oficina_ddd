package br.com.fiap.adapters.out.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ServicoPersistenceAdapter.class)
class ServicoPersistenceAdapterTest {

    @Autowired
    private ServicoPersistenceAdapter servicoPersistenceAdapter;

    private Servico novoServico(String nome) {
        return new Servico(null, nome, "Descrição", new Preco(BigDecimal.valueOf(150.00)), TipoServico.MANUTENCAO, 60);
    }

    @Test
    @DisplayName("Deve salvar um serviço e retorná-lo com ID gerado")
    void deveSalvarServico() {
        Servico salvo = servicoPersistenceAdapter.salvar(novoServico("Troca de Óleo"));

        assertNotNull(salvo.getId());
        assertEquals("Troca de Óleo", salvo.getNome());
    }

    @Test
    @DisplayName("Deve buscar todos os serviços cadastrados")
    void deveBuscarTodos() {
        servicoPersistenceAdapter.salvar(novoServico("Troca de Óleo"));
        servicoPersistenceAdapter.salvar(novoServico("Alinhamento"));

        List<Servico> servicos = servicoPersistenceAdapter.buscarTodos();

        assertEquals(2, servicos.size());
    }

    @Test
    @DisplayName("Deve buscar serviço por nome")
    void deveBuscarPorNome() {
        servicoPersistenceAdapter.salvar(novoServico("Troca de Óleo"));

        Optional<Servico> resultado = servicoPersistenceAdapter.buscarPorNome("Troca de Óleo");

        assertTrue(resultado.isPresent());
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar nome inexistente")
    void deveRetornarVazioAoBuscarNomeInexistente() {
        Optional<Servico> resultado = servicoPersistenceAdapter.buscarPorNome("Troca de Óleo");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve buscar serviço por ID")
    void deveBuscarPorId() {
        Servico salvo = servicoPersistenceAdapter.salvar(novoServico("Troca de Óleo"));

        Optional<Servico> resultado = servicoPersistenceAdapter.buscarPorId(salvo.getId());

        assertTrue(resultado.isPresent());
    }

    @Test
    @DisplayName("Deve excluir serviço por ID")
    void deveExcluirPorId() {
        Servico salvo = servicoPersistenceAdapter.salvar(novoServico("Troca de Óleo"));

        servicoPersistenceAdapter.excluirPorId(salvo.getId());

        assertTrue(servicoPersistenceAdapter.buscarPorId(salvo.getId()).isEmpty());
    }
}
