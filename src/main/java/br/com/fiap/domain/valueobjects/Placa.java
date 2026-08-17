package br.com.fiap.domain.valueobjects;

import java.util.regex.Pattern;

public class Placa {
    private static final Pattern PLACA_PATTERN = Pattern.compile(
        "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$"
    );
    
    private final String valor;

    public Placa(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("A placa não pode ser nula ou vazia.");
        }
        
        String placaFormatada = valor.toUpperCase().replaceAll("[^A-Z0-9]", "");
        
        if (!PLACA_PATTERN.matcher(placaFormatada).matches()) {
            throw new IllegalArgumentException(
                "Formato de placa inválido. Use o formato ABC1234 ou ABC1D23 (Mercosul)."
            );
        }
        
        this.valor = placaFormatada;
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Placa placa = (Placa) o;
        return valor.equals(placa.valor);
    }

    @Override
    public int hashCode() {
        return valor.hashCode();
    }

    @Override
    public String toString() {
        return valor;
    }
}
