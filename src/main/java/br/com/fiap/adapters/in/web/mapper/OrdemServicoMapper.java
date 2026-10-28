package br.com.fiap.adapters.in.web.mapper;

import java.util.Collections;
import java.util.List;

import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoResponse;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.RealizarDiagnosticoRequest;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.ports.in.ItemDiagnostico;
import br.com.fiap.ports.in.CriarOrdemServicoCommand;

public class OrdemServicoMapper {

    public static CriarOrdemServicoCommand toCommand(OrdemServicoCreateRequest dto) {
        return new CriarOrdemServicoCommand(dto.clienteId(), dto.veiculoId(), dto.observacoes(),
                dto.servicos() == null ? List.of() : dto.servicos().stream()
                        .map(item -> new ItemDiagnostico(item.servicoId(), item.quantidade())).toList(),
                dto.pecas() == null ? List.of() : dto.pecas().stream()
                        .map(item -> new ItemDiagnostico(item.pecaId(), item.quantidade())).toList());
    }

    public static List<ItemDiagnostico> toItensServicoDiagnostico(RealizarDiagnosticoRequest dto) {
        if (dto.servicos() == null) {
            return Collections.emptyList();
        }
        return dto.servicos().stream()
                .map(item -> new ItemDiagnostico(item.servicoId(), item.quantidade()))
                .toList();
    }

    public static List<ItemDiagnostico> toItensPecaDiagnostico(RealizarDiagnosticoRequest dto) {
        if (dto.pecas() == null) {
            return Collections.emptyList();
        }
        return dto.pecas().stream()
                .map(item -> new ItemDiagnostico(item.pecaId(), item.quantidade()))
                .toList();
    }

    public static OrdemServicoResponse toResponse(OrdemServico os) {
        return new OrdemServicoResponse(os);
    }

    public static List<OrdemServicoResponse> toResponseList(List<OrdemServico> osList) {
        return osList.stream().map(OrdemServicoMapper::toResponse).toList();
    }

}
