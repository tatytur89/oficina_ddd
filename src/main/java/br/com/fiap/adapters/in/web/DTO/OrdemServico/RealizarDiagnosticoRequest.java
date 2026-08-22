package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

@Schema(description = "Resultado do diagnóstico: transiciona a OS para EM_DIAGNOSTICO, registra os serviços/peças identificados e a previsão de entrega")
public record RealizarDiagnosticoRequest(

    @Schema(description = "Serviços identificados no diagnóstico")
    @Valid
    List<AdicionarServicoRequest> servicos,

    @Schema(description = "Peças identificadas no diagnóstico")
    @Valid
    List<AdicionarPecaRequest> pecas,

    @Schema(description = "Previsão de entrega, estimada agora que se sabe o que precisa ser feito", example = "2026-08-20T18:00:00")
    LocalDateTime dataPrevistaEntrega

) {}
