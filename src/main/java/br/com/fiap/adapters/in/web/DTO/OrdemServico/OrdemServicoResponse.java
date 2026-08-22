package br.com.fiap.adapters.in.web.DTO.OrdemServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.entities.PecaOS;
import br.com.fiap.domain.entities.ServicoOS;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados retornados de uma Ordem de Serviço")
public record OrdemServicoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long clienteId,
        @Schema(example = "1") Long veiculoId,
        @Schema(example = "RECEBIDA") String status,
        @Schema(example = "2026-08-12T10:30:00") LocalDateTime dataAbertura,
        @Schema(example = "2026-08-20T18:00:00") LocalDateTime dataPrevistaEntrega,
        @Schema(example = "2026-08-18T16:45:00") LocalDateTime dataConclusao,
        @Schema(example = "Cliente relata barulho no freio dianteiro ao frear") String observacoes,
        @Schema(example = "250.00") BigDecimal valorServicos,
        @Schema(example = "91.80") BigDecimal valorPecas,
        @Schema(example = "341.80") BigDecimal valorTotal,
        List<ServicoOSResponse> servicos,
        List<PecaOSResponse> pecas,
        @Schema(description = "Chave de acesso para a página pública de acompanhamento/aprovação/avaliação do cliente", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") String chaveAcesso,
        @Schema(description = "Nota de avaliação do cliente (1-5), preenchida após a entrega", example = "5") Integer notaAvaliacao,
        @Schema(description = "Comentário de avaliação do cliente", example = "Ótimo atendimento!") String comentarioAvaliacao
) {
    public OrdemServicoResponse(OrdemServico os) {
        this(
            os.getId(),
            os.getClienteId(),
            os.getVeiculoId(),
            os.getStatus().name(),
            os.getDataAbertura(),
            os.getDataPrevistaEntrega(),
            os.getDataConclusao(),
            os.getObservacoes(),
            os.getValorServicos().getValor(),
            os.getValorPecas().getValor(),
            os.getValorTotal().getValor(),
            os.getServicos().stream().map(ServicoOSResponse::new).toList(),
            os.getPecas().stream().map(PecaOSResponse::new).toList(),
            os.getChaveAcesso(),
            os.getNotaAvaliacao(),
            os.getComentarioAvaliacao()
        );
    }

    @Schema(description = "Serviço vinculado à OS")
    public record ServicoOSResponse(
            @Schema(example = "1") Long servicoId,
            @Schema(example = "Troca de Óleo") String nomeServico,
            @Schema(example = "1") int quantidade,
            @Schema(example = "150.00") BigDecimal precoUnitario,
            @Schema(example = "150.00") BigDecimal valorTotal
    ) {
        public ServicoOSResponse(ServicoOS servico) {
            this(
                servico.getServicoId(),
                servico.getNomeServico(),
                servico.getQuantidade(),
                servico.getPrecoUnitario().getValor(),
                servico.getValorTotal().getValor()
            );
        }
    }

    @Schema(description = "Peça vinculada à OS")
    public record PecaOSResponse(
            @Schema(example = "1") Long pecaId,
            @Schema(example = "Filtro de Óleo") String nomePeca,
            @Schema(example = "FIL001") String codigoPeca,
            @Schema(example = "2") int quantidade,
            @Schema(example = "45.90") BigDecimal precoUnitario,
            @Schema(example = "91.80") BigDecimal valorTotal
    ) {
        public PecaOSResponse(PecaOS peca) {
            this(
                peca.getPecaId(),
                peca.getNomePeca(),
                peca.getCodigoPeca(),
                peca.getQuantidade(),
                peca.getPrecoUnitario().getValor(),
                peca.getValorTotal().getValor()
            );
        }
    }
}
