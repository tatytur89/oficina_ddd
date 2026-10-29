package br.com.fiap.adapters.out.persistence;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: run only against the disposable PostgreSQL namespace created by the database smoke test. */
@EnabledIfEnvironmentVariable(named = "OFICINA_TEST_DATABASE_URL", matches = ".+")
class PostgreSQLSchemaIntegrationTest {
    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        assertNotNull(value, "Required test environment variable: " + name);
        assertFalse(value.isBlank(), "Empty test environment variable: " + name);
        return value;
    }

    private SessionFactory validateSchema() {
        return new Configuration()
                .addAnnotatedClass(ClienteJpaEntity.class)
                .addAnnotatedClass(VeiculoJpaEntity.class)
                .addAnnotatedClass(ServicoJpaEntity.class)
                .addAnnotatedClass(PecaJpaEntity.class)
                .addAnnotatedClass(UsuarioJpaEntity.class)
                .addAnnotatedClass(OrdemServicoJpaEntity.class)
                .addAnnotatedClass(ServicoOSJpaEntity.class)
                .addAnnotatedClass(PecaOSJpaEntity.class)
                .setPhysicalNamingStrategy(new PhysicalNamingStrategySnakeCaseImpl())
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", requiredEnvironment("OFICINA_TEST_DATABASE_URL"))
                .setProperty("hibernate.connection.username", requiredEnvironment("OFICINA_TEST_DATABASE_USER"))
                .setProperty("hibernate.connection.password", requiredEnvironment("OFICINA_TEST_DATABASE_PASSWORD"))
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
    }

    @Test
    void migratedPostgreSQLSchemaMatchesAllJpaEntitiesWithoutCreatingTables() {
        try (var factory = validateSchema()) {
            assertTrue(factory.isOpen());
        }
    }

    @Test
    void postgresPersistsCatalogSnapshotsDecisionAndStockWithinOneTransaction() {
        try (var factory = validateSchema(); var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            try {
                var cliente = new ClienteJpaEntity(null, "Cliente schema", "12345678909", "schema@example.com", "11999999999");
                session.persist(cliente);
                var veiculo = new VeiculoJpaEntity(null, "Toyota", "Corolla", 2022, "ABC1D23", cliente.getId());
                session.persist(veiculo);
                var servico = new ServicoJpaEntity(null, "Revisao " + UUID.randomUUID(), "Revisao", new BigDecimal("100.00"), "REVISAO", 60);
                session.persist(servico);
                var peca = new PecaJpaEntity(null, "Filtro", "Filtro", "F-" + UUID.randomUUID(), new BigDecimal("25.00"), 5, 1);
                session.persist(peca);
                var os = new OrdemServicoJpaEntity(null, cliente.getId(), veiculo.getId(), "AGUARDANDO_APROVACAO",
                        LocalDateTime.now(), null, null, "Schema real", new BigDecimal("100.00"),
                        new BigDecimal("50.00"), new BigDecimal("150.00"), UUID.randomUUID().toString(), null, null);
                os.setVersaoOrcamento(2L);
                os.setServicos(new ArrayList<>(List.of(new ServicoOSJpaEntity(null, servico.getId(), servico.getNome(),
                        1, new BigDecimal("100.00"), new BigDecimal("100.00")))));
                os.setPecas(new ArrayList<>(List.of(new PecaOSJpaEntity(null, peca.getId(), peca.getNome(), peca.getCodigo(),
                        2, new BigDecimal("25.00"), new BigDecimal("50.00")))));
                session.persist(os);
                session.flush();
                session.clear();

                var loaded = session.find(OrdemServicoJpaEntity.class, os.getId());
                assertEquals(2L, loaded.getVersaoOrcamento());
                assertNull(loaded.getDecisaoOrcamento());
                assertEquals(1, loaded.getServicos().size());
                assertEquals(2, loaded.getPecas().getFirst().getQuantidade());
                assertEquals(0, new BigDecimal("150.00").compareTo(loaded.getValorTotal()));
                loaded.setDecisaoOrcamento(true);
                loaded.setStatus("EM_EXECUCAO");
                session.find(PecaJpaEntity.class, peca.getId()).setQuantidadeEstoque(3);
                session.flush();
                session.clear();

                var approved = session.find(OrdemServicoJpaEntity.class, os.getId());
                assertEquals(Boolean.TRUE, approved.getDecisaoOrcamento());
                assertEquals("EM_EXECUCAO", approved.getStatus());
                assertEquals(3, session.find(PecaJpaEntity.class, peca.getId()).getQuantidadeEstoque());
            } finally {
                if (transaction.isActive()) transaction.rollback();
            }
        }
    }

    @Test
    void postgresRejectsDuplicateCustomerDocuments() throws Exception {
        try (var connection = DriverManager.getConnection(requiredEnvironment("OFICINA_TEST_DATABASE_URL"),
                requiredEnvironment("OFICINA_TEST_DATABASE_USER"), requiredEnvironment("OFICINA_TEST_DATABASE_PASSWORD"))) {
            connection.setAutoCommit(false);
            try (var insert = connection.prepareStatement("INSERT INTO clientes (nome, documento) VALUES (?, ?)")) {
                insert.setString(1, "Cliente constraint");
                insert.setString(2, "98765432100");
                insert.executeUpdate();
                SQLException duplicate = assertThrows(SQLException.class, insert::executeUpdate);
                assertEquals("23505", duplicate.getSQLState());
            } finally {
                connection.rollback();
            }
        }
    }
}
