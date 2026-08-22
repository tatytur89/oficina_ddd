package br.com.fiap.adapters.in.web.mapper;

import java.util.Collections;
import java.util.List;

import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.OrdemServico.OrdemServicoResponse;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.ports.in.ItemQuantidade;

public class OrdemServicoMapper {

    public static OrdemServico toDomain(OrdemServicoCreateRequest dto) {
        return new OrdemServico(
                null,
                dto.clienteId(),
                dto.veiculoId(),
                null,
                null,
                dto.dataPrevistaEntrega(),
                null,
                dto.observacoes(),
                null,
                null,
                null,
                null,
                null
        );
    }

    public static List<ItemQuantidade> toItensServicos(OrdemServicoCreateRequest dto) {
        if (dto.servicos() == null) {
            return Collections.emptyList();
        }
        return dto.servicos().stream()
                .map(item -> new ItemQuantidade(item.servicoId(), item.quantidade()))
                .toList();
    }

    public static List<ItemQuantidade> toItensPecas(OrdemServicoCreateRequest dto) {
        if (dto.pecas() == null) {
            return Collections.emptyList();
        }
        return dto.pecas().stream()
                .map(item -> new ItemQuantidade(item.pecaId(), item.quantidade()))
                .toList();
    }

    public static OrdemServicoResponse toResponse(OrdemServico os) {
        return new OrdemServicoResponse(os);
    }

    public static List<OrdemServicoResponse> toResponseList(List<OrdemServico> osList) {
        return osList.stream().map(OrdemServicoMapper::toResponse).toList();
    }

}
