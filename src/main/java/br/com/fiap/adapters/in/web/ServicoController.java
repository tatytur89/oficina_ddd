package br.com.fiap.adapters.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.adapters.in.web.DTO.Dados.Response;
import br.com.fiap.adapters.in.web.DTO.Servico.ServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Servico.ServicoResponse;
import br.com.fiap.adapters.in.web.DTO.Servico.ServicoUpdateRequest;
import br.com.fiap.adapters.in.web.mapper.ServicoMapper;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.ports.in.ServicoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/servicos")
@Tag(
    name = "Serviços",
    description = "Endpoints para gestão do catálogo de serviços de manutenção"
)
public class ServicoController {

    private final ServicoUseCase servicoUseCase;

    public ServicoController(ServicoUseCase servicoUseCase) {
        this.servicoUseCase = servicoUseCase;
    }

    @Operation(
        summary = "Cadastrar um novo serviço",
        description = """
            Cria um novo serviço de manutenção no catálogo.
            
            **Tipos de serviço disponíveis:**
            - REVISAO - Revisão geral
            - MANUTENCAO - Manutenção preventiva/corretiva
            - TROCA_PECA - Troca de peças
            - ALINHAMENTO - Alinhamento de direção
            - BALANCEAMENTO - Balanceamento de rodas
            - MECANICA_GERAL - Mecânica em geral
            - ELETRICA - Serviços elétricos
            - SUSPENSAO - Serviços de suspensão
            - FREIOS - Serviços de freios
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Serviço cadastrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                            "status": "success",
                            "message": "Serviço cadastrado com sucesso",
                            "dados": {
                                "id": 1,
                                "nome": "Troca de Óleo",
                                "descricao": "Troca de óleo do motor 1.0 com filtro",
                                "preco": 150.00,
                                "tipo": "MANUTENCAO",
                                "tempoEstimadoMinutos": 60
                            }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(examples = @ExampleObject(
                value = "{\"status\": \"error\", \"message\": \"nome: O nome é obrigatório\", \"dados\": null}"))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe um serviço com o nome informado",
            content = @Content(examples = @ExampleObject(
                value = "{\"status\": \"error\", \"message\": \"Serviço com nome informado já existe.\", \"dados\": null}"))
        )
    })
    @PostMapping
    public ResponseEntity<Response<ServicoResponse>> cadastrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados do serviço a ser cadastrado",
                required = true
            )
            @Valid @RequestBody ServicoCreateRequest requestDTO) {

        Servico servicoDomain = ServicoMapper.toDomain(requestDTO);

        Servico servicoSalvo = servicoUseCase.cadastrarServico(servicoDomain);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Response<>("success", "Serviço cadastrado com sucesso", ServicoMapper.toResponse(servicoSalvo)));
    }

    @Operation(
        summary = "Listar todos os serviços",
        description = "Retorna uma lista com todos os serviços disponíveis no catálogo."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de serviços retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<Response<List<ServicoResponse>>> listarTodos() {
        List<Servico> servicos = servicoUseCase.listarTodos();

        return ResponseEntity.ok(new Response<>("success", "Serviços listados com sucesso", ServicoMapper.toResponseList(servicos)));
    }

    @Operation(
        summary = "Buscar serviço por ID",
        description = "Retorna os dados completos de um serviço específico pelo seu ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Serviço encontrado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Serviço não encontrado com ID: 1\", \"dados\": null}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response<ServicoResponse>> buscarPorId(
            @Parameter(
                description = "ID do serviço",
                example = "1",
                required = true
            )
            @PathVariable Long id) {

        Servico servico = servicoUseCase.buscarPorId(id);

        return ResponseEntity.ok(new Response<>("success", "Serviço encontrado com sucesso", ServicoMapper.toResponse(servico)));
    }

    @Operation(summary = "Atualizar um serviço existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"nome: O nome é obrigatório\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Serviço não encontrado com ID: 1\", \"dados\": null}"))),
        @ApiResponse(responseCode = "409", description = "Já existe um serviço com o nome informado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Serviço com nome informado já existe.\", \"dados\": null}")))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Response<ServicoResponse>> atualizar(
            @Parameter(description = "ID do serviço", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ServicoUpdateRequest requestDTO) {

        Servico servicoDomain = ServicoMapper.toDomain(id, requestDTO);

        Servico servicoAtualizado = servicoUseCase.atualizarServico(servicoDomain);

        return ResponseEntity.ok(new Response<>("success", "Serviço atualizado com sucesso", ServicoMapper.toResponse(servicoAtualizado)));
    }

    @Operation(summary = "Excluir um serviço")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Serviço excluído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Serviço não encontrado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Serviço não encontrado com ID: 1\", \"dados\": null}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> excluir(
            @Parameter(description = "ID do serviço", example = "1")
            @PathVariable Long id) {

        servicoUseCase.excluirServico(id);

        return ResponseEntity.ok(new Response<>("success", "Serviço excluído com sucesso", null));
    }
}
