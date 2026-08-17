package br.com.fiap.adapters.in.web;

import java.math.BigDecimal;

import br.com.fiap.domain.entities.Peca;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma peça")
public class PecaResponseDTO {

    @Schema(
        description = "Identificador único da peça",
        example = "1"
    )
    private Long id;

    @Schema(
        description = "Nome da peça ou insumo",
        example = "Filtro de Óleo"
    )
    private String nome;

    @Schema(
        description = "Descrição detalhada da peça",
        example = "Filtro de óleo para motor 1.0 - Marca Premium"
    )
    private String descricao;

    @Schema(
        description = "Código interno da peça",
        example = "FIL001"
    )
    private String codigo;

    @Schema(
        description = "Preço unitário da peça em reais (R$)",
        example = "45.90"
    )
    private BigDecimal preco;

    @Schema(
        description = "Quantidade atual em estoque",
        example = "50"
    )
    private Integer quantidadeEstoque;

    @Schema(
        description = "Estoque mínimo configurado",
        example = "10"
    )
    private Integer estoqueMinimo;

    @Schema(
        description = "Indica se o estoque está abaixo do mínimo",
        example = "false"
    )
    private boolean estoqueBaixo;

    public PecaResponseDTO(Peca peca) {
        this.id = peca.getId();
        this.nome = peca.getNome();
        this.descricao = peca.getDescricao();
        this.codigo = peca.getCodigo();
        this.preco = peca.getPreco().getValor();
        this.quantidadeEstoque = peca.getQuantidadeEstoque();
        this.estoqueMinimo = peca.getEstoqueMinimo();
        this.estoqueBaixo = peca.estoqueBaixo();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCodigo() { return codigo; }
    public BigDecimal getPreco() { return preco; }
    public Integer getQuantidadeEstoque() { return quantidadeEstoque; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public boolean isEstoqueBaixo() { return estoqueBaixo; }
}
