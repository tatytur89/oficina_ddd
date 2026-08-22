package br.com.fiap.adapters.in.web.mapper;

import java.util.List;

import br.com.fiap.adapters.in.web.DTO.Veiculo.VeiculoCreateRequest;
import br.com.fiap.adapters.in.web.DTO.Veiculo.VeiculoResponse;
import br.com.fiap.adapters.in.web.DTO.Veiculo.VeiculoUpdateRequest;
import br.com.fiap.domain.entities.Veiculo;

public class VeiculoMapper {

    public static Veiculo toDomain(VeiculoCreateRequest dto) {
        return new Veiculo(null, dto.marca(), dto.modelo(), dto.ano(), dto.placa(), dto.clienteId());
    }

    public static Veiculo toDomain(Long id, VeiculoUpdateRequest dto) {
        return new Veiculo(id, dto.marca(), dto.modelo(), dto.ano(), dto.placa(), dto.clienteId());
    }

    public static VeiculoResponse toResponse(Veiculo veiculo) {
        return new VeiculoResponse(veiculo);
    }

    public static List<VeiculoResponse> toResponseList(List<Veiculo> veiculos) {
        return veiculos.stream().map(VeiculoMapper::toResponse).toList();
    }

}
