package br.com.fiap.adapters.in.web.mapper;

import java.util.List;

import br.com.fiap.adapters.in.web.DTO.Peca.PecaCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Peca.PecaResponse;
import br.com.fiap.adapters.in.web.DTO.Peca.PecaUpdateRequest;
import br.com.fiap.domain.entities.Peca;
import br.com.fiap.domain.valueobjects.Preco;

public class PecaMapper {

    public static Peca toDomain(PecaCreateRequest dto) {
        return new Peca(
                null,
                dto.nome(),
                dto.descricao(),
                dto.codigo(),
                new Preco(dto.preco()),
                dto.quantidadeEstoque(),
                dto.estoqueMinimo()
        );
    }

    // quantidadeEstoque vem null de propósito; PecaService.atualizarPeca deve preservar o valor existente
    public static Peca toDomain(Long id, PecaUpdateRequest dto) {
        return new Peca(
                id,
                dto.nome(),
                dto.descricao(),
                dto.codigo(),
                new Preco(dto.preco()),
                null,
                dto.estoqueMinimo()
        );
    }

    public static PecaResponse toResponse(Peca peca) {
        return new PecaResponse(peca);
    }

    public static List<PecaResponse> toResponseList(List<Peca> pecas) {
        return pecas.stream().map(PecaMapper::toResponse).toList();
    }

}
