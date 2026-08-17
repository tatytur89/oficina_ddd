package br.com.fiap.ports.in;

import java.util.List;
import java.util.Optional;

import br.com.fiap.domain.entities.Peca;

public interface PecaUseCase {
    Peca cadastrarPeca(Peca peca);
    List<Peca> listarTodas();
    Optional<Peca> buscarPorId(Long id);
    Optional<Peca> buscarPorCodigo(String codigo);
    void baixarEstoque(Long pecaId, int quantidade);
    void reporEstoque(Long pecaId, int quantidade);
    List<Peca> listarEstoqueBaixo();
}
