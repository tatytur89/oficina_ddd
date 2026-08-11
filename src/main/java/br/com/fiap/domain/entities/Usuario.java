package br.com.fiap.domain.entities;

public class Usuario {
    private Long id;
    private String username;
    private String password; // Guardará o hash, nunca a senha em texto claro
    private String role;

    public Usuario(Long id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}
