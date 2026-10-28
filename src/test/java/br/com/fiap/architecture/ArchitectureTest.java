package br.com.fiap.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final Path SOURCES = Path.of("src/main/java/br/com/fiap");
    private static final Pattern IMPORT = Pattern.compile("(?m)^\\s*import\\s+(?:static\\s+)?([^;]+);");

    @Test
    void applicationNaoDeveDependerDeAdapters() throws IOException {
        assertSemDependencias("application", List.of("br.com.fiap.adapters."));
    }

    @Test
    void dominioEPortasNaoDevemDependerDeFrameworksOuAdapters() throws IOException {
        var proibidos = List.of("org.springframework.", "jakarta.", "javax.persistence.",
                "com.auth0.", "org.hibernate.", "br.com.fiap.adapters.");
        assertSemDependencias("domain", proibidos);
        assertSemDependencias("ports", proibidos);
    }

    private void assertSemDependencias(String camada, List<String> proibidos) throws IOException {
        Path diretorio = SOURCES.resolve(camada);
        assertTrue(Files.isDirectory(diretorio), "Fontes da camada não encontradas: " + diretorio);
        var violacoes = new ArrayList<String>();
        try (var arquivos = Files.walk(diretorio)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                var imports = IMPORT.matcher(Files.readString(arquivo));
                while (imports.find()) {
                    String dependencia = imports.group(1);
                    if (proibidos.stream().anyMatch(dependencia::startsWith)) {
                        violacoes.add(arquivo + " -> " + dependencia);
                    }
                }
            }
        }
        assertTrue(violacoes.isEmpty(), () -> "Dependências que violam os limites hexagonais:\n"
                + String.join("\n", violacoes));
    }
}
