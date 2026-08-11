package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.in.ClienteUseCase;
import br.com.fiap.ports.out.ClienteRepositoryPort;

@Service
public class ClienteService implements ClienteUseCase {

    private final ClienteRepositoryPort clienteRepositoryPort;

    public ClienteService(ClienteRepositoryPort clienteRepositoryPort) {
        this.clienteRepositoryPort = clienteRepositoryPort;
    }

    @Override
    public Cliente cadastrarCliente(Cliente cliente) {
        Optional<Cliente> existente = clienteRepositoryPort.buscarPorDocumento(cliente.getDocumento());
        if (existente.isPresent()) {
            throw new ResourceAlreadyExistsException("Cliente com CPF/CNPJ informado já existe.");
        }
        return clienteRepositoryPort.salvar(cliente);
    }

    @Override
    public List<Cliente> listarTodos() {
        return clienteRepositoryPort.buscarTodos();
    }

    @Override
    public Optional<Cliente> buscarPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("O documento para busca não pode ser nulo ou vazio");
        }
        // Garante a busca apenas pelos números, limpando máscaras de CPF/CNPJ
        String documentoLimpo = documento.replaceAll("\\D", "");
        return clienteRepositoryPort.buscarPorDocumento(documentoLimpo);
    }


}
