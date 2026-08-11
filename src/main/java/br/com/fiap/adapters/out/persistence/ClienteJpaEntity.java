package br.com.fiap.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class ClienteJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false)
    private String documento;

    private String email;

    private String telefone;

    public ClienteJpaEntity() {
    }

    public ClienteJpaEntity(Long id, String nome, String documento, String email, String telefone) {
        this.id = id;
        this.nome = nome;
        this.documento = documento;
        this.email = email;
        this.telefone = telefone;
    }

    public Long getId() {return id;}
    public String getNome() {return nome;}
    public String getDocumento() {return documento;}
    public String getEmail() {return email;}
    public String getTelefone() {return telefone;}

}
