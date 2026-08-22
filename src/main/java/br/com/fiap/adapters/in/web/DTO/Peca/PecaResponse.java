package br.com.fiap.adapters.in.web.DTO.Peca;

import java.math.BigDecimal;

import br.com.fiap.domain.entities.Peca;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma peça")
public record PecaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Filtro de Óleo") String nome,
        @Schema(example = "Filtro de óleo para motor 1.0 - Marca Premium") String descricao,
        @Schema(example = "FIL001") String codigo,
        @Schema(example = "45.90") BigDecimal preco,
        @Schema(example = "50") Integer quantidadeEstoque,
        @Schema(example = "10") Integer estoqueMinimo,
        @Schema(example = "false") boolean estoqueBaixo
) {
    public PecaResponse(Peca peca) {
        this(
            peca.getId(),
            peca.getNome(),
            peca.getDescricao(),
            peca.getCodigo(),
            peca.getPreco().getValor(),
            peca.getQuantidadeEstoque(),
            peca.getEstoqueMinimo(),
            peca.estoqueBaixo()
        );
    }
}
