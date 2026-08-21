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

	@Override
	public Cliente atualizarCliente(Cliente client) {
		
		if(client == null || client.getId() == null) {
			throw new IllegalArgumentException("O cliente e o ID são obrigatórios");
		}
		
		Cliente findedCliente = clienteRepositoryPort.buscarPorId(client.getId())
							.orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com ID: " 
		+ client.getId()));
		
		Optional<Cliente> clienteWithSameDocument = clienteRepositoryPort.buscarPorDocumento(client.getDocumento());
		
		if(clienteWithSameDocument.isPresent() && !clienteWithSameDocument.get().getId().equals(findedCliente.getId())) {
			 throw new ResourceAlreadyExistsException("Cliente com CPF/CNPJ informado já existe.");
		}
		
		Cliente updatedCliente = new Cliente(
				findedCliente.getId(), 			//id
				client.getNome(), 				//nome
				client.getDocumento(), 			//documento
				client.getEmail(), 				//email
				client.getTelefone()  			//telefone
		);
		
		return clienteRepositoryPort.salvar(updatedCliente);
	}

	@Override
	public void excluirCliente(Long id) {
		
		if(id == null) throw new IllegalArgumentException("O ID do cliente é obrigatório");
		
		 clienteRepositoryPort.buscarPorId(id).orElseThrow(() ->
                 new IllegalArgumentException("Cliente não encontrado com ID: " + id));

		 clienteRepositoryPort.excluirPorId(id);
		
	}


}
