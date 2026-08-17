package br.com.fiap.adapters.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.ports.in.PecaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PecaResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                        {
                            "id": 1,
                            "nome": "Filtro de Óleo",
                            "descricao": "Filtro de óleo para motor 1.0",
                            "codigo": "FIL001",
                            "preco": 45.90,
                            "quantidadeEstoque": 50,
                            "estoqueMinimo": 10,
                            "estoqueBaixo": false
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Já existe uma peça com o código informado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @PostMapping
    public ResponseEntity<PecaResponseDTO> cadastrar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dados da peça a ser cadastrada",
                required = true
            )
            @Valid @RequestBody PecaRequestDTO requestDTO) {
        
        Peca pecaDomain = new Peca(
            null,
            requestDTO.getNome(),
            requestDTO.getDescricao(),
            requestDTO.getCodigo(),
            new Preco(requestDTO.getPreco()),
            requestDTO.getQuantidadeEstoque(),
            requestDTO.getEstoqueMinimo()
        );

        Peca pecaCadastrada = pecaUseCase.cadastrarPeca(pecaDomain);
        PecaResponseDTO responseDTO = new PecaResponseDTO(pecaCadastrada);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @Operation(
        summary = "Listar todas as peças",
        description = "Retorna uma lista com todas as peças cadastradas no estoque."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de peças retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PecaResponseDTO.class)
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<PecaResponseDTO>> listarTodas() {
        List<Peca> pecas = pecaUseCase.listarTodas();

        List<PecaResponseDTO> responseDTOs = pecas.stream()
                .map(PecaResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(
        summary = "Buscar peça por ID",
        description = "Retorna os dados completos de uma peça específica pelo seu ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Peça encontrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PecaResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Peça não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<PecaResponseDTO> buscarPorId(
            @Parameter(
                description = "ID da peça",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        
        return pecaUseCase.buscarPorId(id)
                .map(peca -> ResponseEntity.ok(new PecaResponseDTO(peca)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Buscar peça por código",
        description = "Retorna os dados de uma peça pelo seu código interno."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Peça encontrada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PecaResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Peça não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<PecaResponseDTO> buscarPorCodigo(
            @Parameter(
                description = "Código interno da peça",
                example = "FIL001",
                required = true
            )
            @PathVariable String codigo) {
        
        return pecaUseCase.buscarPorCodigo(codigo)
                .map(peca -> ResponseEntity.ok(new PecaResponseDTO(peca)))
                .orElse(ResponseEntity.notFound().build());
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
        @ApiResponse(
            responseCode = "200",
            description = "Estoque reposto com sucesso",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Peça não encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiErrorResponse.class)
            )
        )
    })
    @PutMapping("/{id}/repor-estoque/{quantidade}")
    public ResponseEntity<Void> reporEstoque(
            @Parameter(
                description = "ID da peça",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            
            @Parameter(
                description = "Quantidade a ser adicionada ao estoque",
                example = "10",
                required = true
            )
            @PathVariable int quantidade) {
        
        pecaUseCase.reporEstoque(id, quantidade);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Listar peças com estoque baixo",
        description = """
            Retorna todas as peças cujo estoque atual está igual ou abaixo do estoque mínimo configurado.
            
            **Uso recomendado:** Executar periodicamente para gerar alertas de reposição.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de peças com estoque baixo",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PecaResponseDTO.class)
            )
        )
    })
    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<PecaResponseDTO>> listarEstoqueBaixo() {
        List<Peca> pecas = pecaUseCase.listarEstoqueBaixo();

        List<PecaResponseDTO> responseDTOs = pecas.stream()
                .map(PecaResponseDTO::new)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }
}
