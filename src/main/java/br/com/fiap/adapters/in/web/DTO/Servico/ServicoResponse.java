package br.com.fiap.adapters.in.web.DTO.Servico;

import java.math.BigDecimal;

import br.com.fiap.domain.entities.Servico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de um serviço")
public record ServicoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Troca de Óleo") String nome,
        @Schema(example = "Troca de óleo do motor 1.0 com filtro de óleo") String descricao,
        @Schema(example = "150.00") BigDecimal preco,
        @Schema(example = "MANUTENCAO") String tipo,
        @Schema(example = "60") Integer tempoEstimadoMinutos
) {
    public ServicoResponse(Servico servico) {
        this(
            servico.getId(),
            servico.getNome(),
            servico.getDescricao(),
            servico.getPreco().getValor(),
            servico.getTipo().name(),
            servico.getTempoEstimadoMinutos()
        );
    }
}
