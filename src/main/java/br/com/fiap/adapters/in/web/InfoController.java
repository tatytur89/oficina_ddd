package br.com.fiap.adapters.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.adapters.in.web.DTO.Info.InfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/info")
@Tag(
    name = "Informações",
    description = "Versão da aplicação em execução"
)
public class InfoController {

    private final String aplicacao;
    private final String commit;
    private final String pod;

    public InfoController(
            @Value("${spring.application.name:oficina-ddd}") String aplicacao,
            @Value("${app.build.commit:local}") String commit,
            @Value("${HOSTNAME:local}") String pod) {
        this.aplicacao = aplicacao;
        this.commit = commit;
        this.pod = pod;
    }

    @Operation(
        summary = "Consultar versão em execução",
        description = "Retorna o commit que gerou a imagem e o pod que atendeu a requisição. Não requer autenticação."
    )
    @GetMapping
    public ResponseEntity<Response<InfoResponse>> info() {
        return ResponseEntity.ok(new Response<>("success", "Aplicação em execução", new InfoResponse(aplicacao, commit, pod)));
    }
}
