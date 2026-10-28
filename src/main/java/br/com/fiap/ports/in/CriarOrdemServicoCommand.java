package br.com.fiap.ports.in;

import java.util.List;

public record CriarOrdemServicoCommand(Long clienteId, Long veiculoId, String observacoes,
                                      List<ItemDiagnostico> servicos, List<ItemDiagnostico> pecas) {
    public CriarOrdemServicoCommand {
        if (clienteId == null || clienteId <= 0 || veiculoId == null || veiculoId <= 0) {
            throw new IllegalArgumentException("Cliente e veículo devem ter IDs positivos.");
        }
        servicos = validarItens(servicos);
        pecas = validarItens(pecas);
    }

    private static List<ItemDiagnostico> validarItens(List<ItemDiagnostico> itens) {
        if (itens == null) {
            return List.of();
        }
        for (ItemDiagnostico item : itens) {
            if (item == null || item.id() == null || item.id() <= 0 || item.quantidade() <= 0) {
                throw new IllegalArgumentException("Cada item deve ter ID e quantidade positivos.");
            }
        }
        return List.copyOf(itens);
    }
}
