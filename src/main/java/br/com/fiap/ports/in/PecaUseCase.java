package br.com.fiap.ports.in;

import java.util.List;

import br.com.fiap.domain.entities.Peca;

public interface PecaUseCase {
    Peca cadastrarPeca(Peca peca);
    List<Peca> listarTodas();
    Peca buscarPorId(Long id);
    Peca buscarPorCodigo(String codigo);
    void baixarEstoque(Long pecaId, int quantidade);
    void reporEstoque(Long pecaId, int quantidade);
    List<Peca> listarEstoqueBaixo();
    Peca atualizarPeca(Peca peca);
    void excluirPeca(Long id);
}
