package br.com.fiap.domain.entities;

import br.com.fiap.domain.valueobjects.Placa;

public class Veiculo {
    private Long id;
    private String marca;
    private String modelo;
    private Integer ano;
    private Placa placa;
    private Long clienteId;

    public Veiculo(Long id, String marca, String modelo, Integer ano, String placa, Long clienteId) {
        this.id = id;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.placa = new Placa(placa);
        this.clienteId = clienteId;
    }

    public Long getId() { return id; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public Integer getAno() { return ano; }
    public String getPlaca() { return placa.getValor(); }
    public Long getClienteId() { return clienteId; }
}
