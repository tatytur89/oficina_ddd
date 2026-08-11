package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Documento;

public class Cliente {
    private Long id;
    private String nome;
    private Documento documento;
    private String email;
    private String telefone;

    public Cliente(Long id, String nome, String documento, String email, String telefone) {
        this.id = id;
        this.nome = nome;
        this.documento = new Documento(documento);
        this.email = email;
        this.telefone = telefone;
    }

    public Long getId() {
        return id;
    }
    public String getNome() {
        return nome;
    }
    public String getDocumento() {
        return documento.getNumero();
    }
    public String getEmail() {
        return email;
    }
    public String getTelefone() {
        return telefone;
    }
}
