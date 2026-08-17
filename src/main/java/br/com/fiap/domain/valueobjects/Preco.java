package br.com.fiap.domain.valueobjects;

import java.math.BigDecimal;

public class Preco {
    private final BigDecimal valor;

    public Preco(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("O valor do preço não pode ser nulo.");
        }
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O valor do preço não pode ser negativo.");
        }
        this.valor = valor;
    }

    public Preco(double valor) {
        this(BigDecimal.valueOf(valor));
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Preco somar(Preco outro) {
        return new Preco(this.valor.add(outro.valor));
    }

    public Preco multiplicar(int quantidade) {
        return new Preco(this.valor.multiply(BigDecimal.valueOf(quantidade)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Preco preco = (Preco) o;
        return valor.compareTo(preco.valor) == 0;
    }

    @Override
    public int hashCode() {
        return valor.hashCode();
    }

    @Override
    public String toString() {
        return "R$ " + valor.toPlainString();
    }
}
