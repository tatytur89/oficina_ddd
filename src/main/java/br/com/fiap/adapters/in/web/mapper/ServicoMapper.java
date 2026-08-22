package br.com.fiap.adapters.in.web.mapper;

import java.util.List;

import br.com.fiap.adapters.in.web.DTO.Servico.ServicoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Servico.ServicoResponse;
import br.com.fiap.adapters.in.web.DTO.Servico.ServicoUpdateRequest;
import br.com.fiap.domain.entities.Servico;
import br.com.fiap.domain.valueobjects.Preco;
import br.com.fiap.domain.valueobjects.TipoServico;

public class ServicoMapper {

    public static Servico toDomain(ServicoCreateRequest dto) {
        return new Servico(
                null,
                dto.nome(),
                dto.descricao(),
                new Preco(dto.preco()),
                TipoServico.valueOf(dto.tipo()),
                dto.tempoEstimadoMinutos()
        );
    }

    public static Servico toDomain(Long id, ServicoUpdateRequest dto) {
        return new Servico(
                id,
                dto.nome(),
                dto.descricao(),
                new Preco(dto.preco()),
                TipoServico.valueOf(dto.tipo()),
                dto.tempoEstimadoMinutos()
        );
    }

    public static ServicoResponse toResponse(Servico servico) {
        return new ServicoResponse(servico);
    }

    public static List<ServicoResponse> toResponseList(List<Servico> servicos) {
        return servicos.stream().map(ServicoMapper::toResponse).toList();
    }

}
