package br.com.fiap.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.out.ClienteRepositoryPort;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepositoryPort repositoryPort;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("Deve cadastrar um cliente com sucesso")
    void deveCadastrarCliente() {
        // Arrange
        Cliente cliente = new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999");
        Cliente clienteSalvo = new Cliente(1L, "Robert", "12345678909", "robert@email.com", "11999999999");
        
        when(repositoryPort.salvar(any(Cliente.class))).thenReturn(clienteSalvo);

        // Act
        Cliente resultado = clienteService.cadastrarCliente(cliente);

        // Assert
        assertNotNull(resultado.getId());
        assertEquals("12345678909", resultado.getDocumento());
        verify(repositoryPort, times(1)).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve buscar cliente por documento com sucesso")
    void deveBuscarPorDocumento() {
        // Arrange
        String docBusca = "12345678909";
        Cliente cliente = new Cliente(1L, "Robert", docBusca, "robert@email.com", "11999999999");
        
        when(repositoryPort.buscarPorDocumento(docBusca)).thenReturn(Optional.of(cliente));

        // Act
      Cliente resultado = clienteService.buscarPorDocumento(docBusca);

        // Assert
        
    }

    @Test
    @DisplayName("Deve lançar ResourceAlreadyExistsException quando documento já existe")
    void deveLancarResourceAlreadyExistsExceptionQuandoDocumentoExiste() {
        // Arrange
        Cliente cliente = new Cliente(null, "Robert", "12345678909", "robert@email.com", "11999999999");
        when(repositoryPort.buscarPorDocumento("12345678909")).thenReturn(Optional.of(cliente));

        // Act / Assert
        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class,
                () -> clienteService.cadastrarCliente(cliente));

        assertEquals("Cliente com CPF/CNPJ informado já existe.", exception.getMessage());
        verify(repositoryPort, never()).salvar(any(Cliente.class));
    }
}
