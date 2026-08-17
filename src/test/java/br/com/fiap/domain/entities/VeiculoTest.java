package br.com.fiap.domain.entities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import br.com.fiap.domain.valueobjects.Placa;

class VeiculoTest {

    @Test
    void deveCriarVeiculoComSucesso() {
        Veiculo veiculo = new Veiculo(1L, "Toyota", "Corolla", 2022, "ABC1D23", 1L);
        
        assertEquals(1L, veiculo.getId());
        assertEquals("Toyota", veiculo.getMarca());
        assertEquals("Corolla", veiculo.getModelo());
        assertEquals(2022, veiculo.getAno());
        assertEquals("ABC1D23", veiculo.getPlaca());
        assertEquals(1L, veiculo.getClienteId());
    }

    @Test
    void deveLancarExcecaoParaPlacaInvalida() {
        assertThrows(IllegalArgumentException.class, 
            () -> new Veiculo(1L, "Toyota", "Corolla", 2022, "INVALID", 1L));
    }

    @Test
    void deveAceitarPlacaMercosul() {
        Veiculo veiculo = new Veiculo(1L, "Honda", "Civic", 2023, "XYZ9A12", 1L);
        assertEquals("XYZ9A12", veiculo.getPlaca());
    }
}
