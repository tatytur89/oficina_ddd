package br.com.fiap.ports.out;

public interface PasswordVerifierPort {
    boolean verificar(String senha, String hash);
}
