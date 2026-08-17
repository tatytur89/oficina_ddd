package br.com.fiap.adapters.in.web;

import java.math.BigDecimal;

import br.com.fiap.domain.entities.Servico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um serviço")
public class ServicoResponseDTO {

    @Schema(
        description = "Identificador único do serviço",
        example = "1"
    )
    private Long id;

    @Schema(
        description = "Nome do serviço de manutenção",
        example = "Troca de Óleo"
    )
    private String nome;

    @Schema(
        description = "Descrição detalhada do serviço",
        example = "Troca de óleo do motor 1.0 com filtro de óleo"
    )
    private String descricao;

    @Schema(
        description = "Preço do serviço em reais (R$)",
        example = "150.00"
    )
    private BigDecimal preco;

    @Schema(
        description = "Tipo do serviço de manutenção",
        example = "MANUTENCAO"
    )
    private String tipo;

    @Schema(
        description = "Tempo estimado de execução em minutos",
        example = "60"
    )
    private Integer tempoEstimadoMinutos;

    public ServicoResponseDTO(Servico servico) {
        this.id = servico.getId();
        this.nome = servico.getNome();
        this.descricao = servico.getDescricao();
        this.preco = servico.getPreco().getValor();
        this.tipo = servico.getTipo().name();
        this.tempoEstimadoMinutos = servico.getTempoEstimadoMinutos();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public String getTipo() { return tipo; }
    public Integer getTempoEstimadoMinutos() { return tempoEstimadoMinutos; }
}
