package br.com.fiap.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

class RuntimeProfilesTest {

    private static final String CONFIG_LOCATION = Path.of("src/main/resources/application.properties")
            .toAbsolutePath().toUri().toString();
    private static final Map<String, String> EXTERNAL_SETTINGS = Map.of(
            "SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/oficina_lab_test",
            "SPRING_DATASOURCE_USERNAME", "lab-user",
            "SPRING_DATASOURCE_PASSWORD", "database-test-password",
            "JWT_SECRET", "exclusive-runtime-profile-test-secret",
            "ADMIN_USER", "lab-admin",
            "ADMIN_PASSWORD", "admin-test-password");

    @Test
    void deveUsarLocalComoPerfilPadraoSemCarregarConfiguracaoH2DosTestes() {
        runner(null).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getDefaultProfiles()).contains("local");
            assertLocal(context.getBean(RuntimeSettings.class), "localhost");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "docker"})
    void deveResolverExecucaoNativaECompose(String profile) {
        runner(profile).run(context -> {
            assertThat(context).hasNotFailed();
            assertLocal(context.getBean(RuntimeSettings.class), profile.equals("docker") ? "postgres" : "localhost");
            if (profile.equals("docker")) {
                assertThat(context.getEnvironment().getActiveProfiles()).contains("docker", "local");
            }
        });
    }

    @Test
    void composeDevePermitirSobrescreverUrlSemPerderDefaultsLocais() {
        runner("docker", "SPRING_DATASOURCE_URL=jdbc:postgresql://custom-postgres:5432/custom")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RuntimeSettings.class).url())
                            .isEqualTo("jdbc:postgresql://custom-postgres:5432/custom");
                    assertThat(context.getBean(RuntimeSettings.class).ddl()).isEqualTo("update");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"lab", "aws"})
    void deveResolverAmbientesGerenciadosSomenteComConfiguracaoExterna(String profile) {
        runner(profile, externalProperties(EXTERNAL_SETTINGS)).run(context -> {
            assertThat(context).hasNotFailed();
            var settings = context.getBean(RuntimeSettings.class);
            assertThat(settings.url()).isEqualTo(EXTERNAL_SETTINGS.get("SPRING_DATASOURCE_URL"));
            assertThat(settings.username()).isEqualTo(EXTERNAL_SETTINGS.get("SPRING_DATASOURCE_USERNAME"));
            assertThat(settings.password()).isEqualTo(EXTERNAL_SETTINGS.get("SPRING_DATASOURCE_PASSWORD"));
            assertThat(settings.jwt()).isEqualTo(EXTERNAL_SETTINGS.get("JWT_SECRET"));
            assertThat(settings.admin()).isEqualTo(EXTERNAL_SETTINGS.get("ADMIN_USER"));
            assertThat(settings.adminPassword()).isEqualTo(EXTERNAL_SETTINGS.get("ADMIN_PASSWORD"));
            assertThat(settings.ddl()).isEqualTo("validate");
            assertThat(settings.showSql()).isFalse();
            assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("local", "docker");
        });
    }

    @ParameterizedTest(name = "{0} exige {1}")
    @MethodSource("missingExternalSettings")
    void deveFalharQuandoUmaConfiguracaoExternaObrigatoriaEstaAusente(String profile, String missingKey) {
        var supplied = new LinkedHashMap<>(EXTERNAL_SETTINGS);
        supplied.remove(missingKey);
        runner(profile, externalProperties(supplied)).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining(missingKey);
        });
    }

    private static Stream<Arguments> missingExternalSettings() {
        return Stream.of("lab", "aws")
                .flatMap(profile -> EXTERNAL_SETTINGS.keySet().stream().map(key -> Arguments.of(profile, key)));
    }

    private ApplicationContextRunner runner(String profile, String... properties) {
        var values = new ArrayList<String>();
        values.add("spring.config.location=" + CONFIG_LOCATION);
        if (profile != null) {
            values.add("spring.profiles.active=" + profile);
        }
        values.addAll(Arrays.asList(properties));
        return new ApplicationContextRunner()
                .withInitializer(context -> {
                    // Isola credenciais e perfis eventualmente definidos na máquina ou no CI.
                    context.getEnvironment().getPropertySources()
                            .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                    context.getEnvironment().getPropertySources()
                            .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                    new ConfigDataApplicationContextInitializer().initialize(context);
                })
                .withPropertyValues(values.toArray(String[]::new))
                .withUserConfiguration(RequiredRuntimeConfiguration.class);
    }

    private static String[] externalProperties(Map<String, String> settings) {
        return settings.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    private void assertLocal(RuntimeSettings settings, String host) {
        assertThat(settings.url()).isEqualTo("jdbc:postgresql://" + host + ":5432/oficina_db");
        assertThat(settings.username()).isEqualTo("oficina_user");
        assertThat(settings.password()).isEqualTo("password123");
        assertThat(settings.admin()).isEqualTo("admin");
        assertThat(settings.adminPassword()).isEqualTo("admin123");
        assertThat(settings.jwt()).isEqualTo("local-development-jwt-secret-at-least-32-bytes");
        assertThat(settings.ddl()).isEqualTo("update");
        assertThat(settings.showSql()).isTrue();
    }

    @Configuration(proxyBeanMethods = false)
    static class RequiredRuntimeConfiguration {
        @Bean
        RuntimeSettings runtimeSettings(Environment environment) {
            return new RuntimeSettings(environment.getRequiredProperty("spring.datasource.url"),
                    environment.getRequiredProperty("spring.datasource.username"),
                    environment.getRequiredProperty("spring.datasource.password"),
                    environment.getRequiredProperty("api.security.token.secret"),
                    environment.getRequiredProperty("app.admin.default-user"),
                    environment.getRequiredProperty("app.admin.default-password"),
                    environment.getRequiredProperty("spring.jpa.hibernate.ddl-auto"),
                    environment.getRequiredProperty("spring.jpa.show-sql", Boolean.class));
        }
    }

    record RuntimeSettings(String url, String username, String password, String jwt, String admin,
                           String adminPassword, String ddl, boolean showSql) {
    }
}
