package br.com.fiap.adapters.in.web.mapper;

import java.util.List;

import br.com.fiap.adapters.in.web.VeiculoRequestDTO;
import br.com.fiap.adapters.in.web.VeiculoResponseDTO;
import br.com.fiap.domain.entities.Veiculo;

public class VeiculoMapper {

    public static Veiculo toDomain(Long id, VeiculoRequestDTO dto) {
        return new Veiculo(
                id,
                dto.getMarca(),
                dto.getModelo(),
                dto.getAno(),
                dto.getPlaca(),
                dto.getClienteId()
        );
    }

    public static VeiculoResponseDTO toResponse(Veiculo veiculo) {
        return new VeiculoResponseDTO(veiculo);
    }

    public static List<VeiculoResponseDTO> toResponseList(List<Veiculo> veiculos) {
        return veiculos.stream().map(VeiculoMapper::toResponse).toList();
    }

}
