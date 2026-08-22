package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import br.com.fiap.ports.in.TempoMedioExecucao;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tempo médio decorrido entre a abertura e a conclusão das ordens de serviço já finalizadas")
public record TempoMedioExecucaoResponse(
        @Schema(example = "182.5") double tempoMedioMinutos,
        @Schema(example = "12") long quantidadeOSConsideradas
) {
    public TempoMedioExecucaoResponse(TempoMedioExecucao tempoMedioExecucao) {
        this(tempoMedioExecucao.tempoMedioMinutos(), tempoMedioExecucao.quantidadeOSConsideradas());
    }
}
