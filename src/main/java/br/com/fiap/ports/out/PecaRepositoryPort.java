package br.com.fiap.ports.out;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Peca;

public interface PecaRepositoryPort {
    Peca salvar(Peca peca);
    List<Peca> buscarTodas();
    Optional<Peca> buscarPorId(Long id);
    Optional<Peca> buscarPorCodigo(String codigo);
    List<Peca> buscarEstoqueBaixo(int estoqueMinimo);
    void excluirPorId(Long id);
}
