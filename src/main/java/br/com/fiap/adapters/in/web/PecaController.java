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
import br.com.fiap.adapters.in.web.DTO.Peca.PecaCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Peca.PecaResponse;
import br.com.fiap.adapters.in.web.DTO.Peca.PecaUpdateRequest;
import br.com.fiap.adapters.in.web.DTO.Peca.ReporEstoqueRequest;
import br.com.fiap.adapters.in.web.mapper.PecaMapper;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.ports.in.PecaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pecas")
@Tag(
    name = "Peças",
    description = "Endpoints para gestão de peças e insumos com controle de estoque"
)
public class PecaController {

    private final PecaUseCase pecaUseCase;

    public PecaController(PecaUseCase pecaUseCase) {
        this.pecaUseCase = pecaUseCase;
    }

    @Operation(
        summary = "Cadastrar uma nova peça",
        description = """
            Cria uma nova peça no estoque da oficina.

            **Regras:**
            - O código da peça deve ser único no sistema
            - O estoque inicial não pode ser negativo
            - O estoque mínimo deve ser definido para alertas automáticos
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Peça cadastrada com sucesso",
            content = @Content(examples = @ExampleObject(
                value = """
                    {
                        "status": "success",
                        "message": "Peça cadastrada com sucesso",
                        "dados": {
                            "id": 1,
                            "nome": "Filtro de Óleo",
                            "descricao": "Filtro de óleo para motor 1.0",
                            "codigo": "FIL001",
                            "preco": 45.90,
                            "quantidadeEstoque": 50,
                            "estoqueMinimo": 10,
                            "estoqueBaixo": false
                        }
                    }
                    """
            ))
        ),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"nome: O nome é obrigatório\", \"dados\": null}"))),
        @ApiResponse(responseCode = "409", description = "Já existe uma peça com o código informado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Peça com código informado já existe.\", \"dados\": null}")))
    })
    @PostMapping
    public ResponseEntity<Response<PecaResponse>> cadastrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados da peça a ser cadastrada",
                required = true
            )
            @Valid @RequestBody PecaCreateRequest requestDTO) {

        Peca pecaDomain = PecaMapper.toDomain(requestDTO);

        Peca pecaCadastrada = pecaUseCase.cadastrarPeca(pecaDomain);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new Response<>("success", "Peça cadastrada com sucesso", PecaMapper.toResponse(pecaCadastrada)));
    }

    @Operation(
        summary = "Listar todas as peças",
        description = "Retorna uma lista com todas as peças cadastradas no estoque."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de peças retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<Response<List<PecaResponse>>> listarTodas() {
        List<Peca> pecas = pecaUseCase.listarTodas();

        return ResponseEntity.ok(new Response<>("success", "Peças listadas com sucesso", PecaMapper.toResponseList(pecas)));
    }

    @Operation(
        summary = "Buscar peça por ID",
        description = "Retorna os dados completos de uma peça específica pelo seu ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peça encontrada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Peça não encontrada com ID: 1\", \"dados\": null}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response<PecaResponse>> buscarPorId(
            @Parameter(description = "ID da peça", example = "1", required = true)
            @PathVariable Long id) {

        Peca peca = pecaUseCase.buscarPorId(id);

        return ResponseEntity.ok(new Response<>("success", "Peça encontrada com sucesso", PecaMapper.toResponse(peca)));
    }

    @Operation(
        summary = "Buscar peça por código",
        description = "Retorna os dados de uma peça pelo seu código interno."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peça encontrada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Peça não encontrada com código: FIL001\", \"dados\": null}")))
    })
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Response<PecaResponse>> buscarPorCodigo(
            @Parameter(description = "Código interno da peça", example = "FIL001", required = true)
            @PathVariable String codigo) {

        Peca peca = pecaUseCase.buscarPorCodigo(codigo);

        return ResponseEntity.ok(new Response<>("success", "Peça encontrada com sucesso", PecaMapper.toResponse(peca)));
    }

    @Operation(summary = "Atualizar uma peça existente",
        description = "Atualiza nome, descrição, código, preço e estoque mínimo. A quantidade em estoque não é alterada por este endpoint — use /repor-estoque para isso."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peça atualizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"nome: O nome é obrigatório\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Peça não encontrada com ID: 1\", \"dados\": null}"))),
        @ApiResponse(responseCode = "409", description = "Já existe uma peça com o código informado", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Peça com código informado já existe.\", \"dados\": null}")))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Response<PecaResponse>> atualizar(
            @Parameter(description = "ID da peça", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody PecaUpdateRequest requestDTO) {

        Peca pecaDomain = PecaMapper.toDomain(id, requestDTO);

        Peca pecaAtualizada = pecaUseCase.atualizarPeca(pecaDomain);

        return ResponseEntity.ok(new Response<>("success", "Peça atualizada com sucesso", PecaMapper.toResponse(pecaAtualizada)));
    }

    @Operation(summary = "Excluir uma peça")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Peça excluída com sucesso"),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Peça não encontrada com ID: 1\", \"dados\": null}")))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> excluir(
            @Parameter(description = "ID da peça", example = "1")
            @PathVariable Long id) {

        pecaUseCase.excluirPeca(id);

        return ResponseEntity.ok(new Response<>("success", "Peça excluída com sucesso", null));
    }

    @Operation(
        summary = "Repor estoque de uma peça",
        description = """
            Adiciona quantidade ao estoque de uma peça.

            **Útil para:**
            - Recebimento de novos produtos
            - Devoluções
            - Correção de inventário
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estoque reposto com sucesso"),
        @ApiResponse(responseCode = "400", description = "Quantidade inválida", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"quantidade: A quantidade deve ser maior que zero\", \"dados\": null}"))),
        @ApiResponse(responseCode = "404", description = "Peça não encontrada", content = @Content(
            examples = @ExampleObject(value = "{\"status\": \"error\", \"message\": \"Peça não encontrada com ID: 1\", \"dados\": null}")))
    })
    @PutMapping("/{id}/repor-estoque")
    public ResponseEntity<Response<Void>> reporEstoque(
            @Parameter(description = "ID da peça", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ReporEstoqueRequest requestDTO) {

        pecaUseCase.reporEstoque(id, requestDTO.quantidade());

        return ResponseEntity.ok(new Response<>("success", "Estoque reposto com sucesso", null));
    }

    @Operation(
        summary = "Listar peças com estoque baixo",
        description = """
            Retorna todas as peças cujo estoque atual está igual ou abaixo do estoque mínimo configurado.

            **Uso recomendado:** Executar periodicamente para gerar alertas de reposição.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de peças com estoque baixo")
    })
    @GetMapping("/estoque-baixo")
    public ResponseEntity<Response<List<PecaResponse>>> listarEstoqueBaixo() {
        List<Peca> pecas = pecaUseCase.listarEstoqueBaixo();

        return ResponseEntity.ok(new Response<>("success", "Peças com estoque baixo listadas com sucesso", PecaMapper.toResponseList(pecas)));
    }
}
