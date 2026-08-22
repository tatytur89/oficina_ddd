package br.com.fiap.application.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.com.fiap.application.exceptions.ResourceAlreadyExistsException;
import br.com.fiap.application.exceptions.ResourceInUseException;
import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.Cliente;
import br.com.fiap.ports.in.ClienteUseCase;
import br.com.fiap.ports.out.ClienteRepositoryPort;
import br.com.fiap.ports.out.OrdemServicoRepositoryPort;
import br.com.fiap.ports.out.VeiculoRepositoryPort;

@Service
public class ClienteService implements ClienteUseCase {

    private final ClienteRepositoryPort clienteRepositoryPort;
    private final VeiculoRepositoryPort veiculoRepositoryPort;
    private final OrdemServicoRepositoryPort osRepositoryPort;

    public ClienteService(ClienteRepositoryPort clienteRepositoryPort, VeiculoRepositoryPort veiculoRepositoryPort, OrdemServicoRepositoryPort osRepositoryPort) {
        this.clienteRepositoryPort = clienteRepositoryPort;
        this.veiculoRepositoryPort = veiculoRepositoryPort;
        this.osRepositoryPort = osRepositoryPort;
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
    public Cliente buscarPorDocumento(String documento) {
    	
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("O documento para busca não pode ser nulo ou vazio");
        }
        
        String documentoLimpo = documento.replaceAll("\\D", "");
        
        return clienteRepositoryPort.buscarPorDocumento(documentoLimpo).orElseThrow(() ->
                        new ResourceNotFoundException("Cliente não encontrado"));
        
    }

	@Override
	public Cliente atualizarCliente(Cliente client) {
		
		if(client == null || client.getId() == null) {
			throw new IllegalArgumentException("O cliente e o ID são obrigatórios");
		}
		
		Cliente findedCliente = clienteRepositoryPort.buscarPorId(client.getId())
                		.orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " 
                		+ client.getId()));
		
		Optional<Cliente> clienteWithSameDocument = clienteRepositoryPort.buscarPorDocumento(client.getDocumento());
		
		if(clienteWithSameDocument.isPresent() && !clienteWithSameDocument.get().getId().equals(findedCliente.getId())) {
			 throw new ResourceAlreadyExistsException("Cliente com CPF/CNPJ informado já existe.");
		}
		
		findedCliente.atualizarDados(client.getNome(), client.getDocumento(), client.getEmail(), client.getTelefone());

		return clienteRepositoryPort.salvar(findedCliente);
	}

	@Override
	public void excluirCliente(Long id) {
		
		if(id == null) throw new IllegalArgumentException("O ID do cliente é obrigatório");

		clienteRepositoryPort.buscarPorId(id).orElseThrow(() ->
        						new ResourceNotFoundException("Cliente não encontrado com ID: " + id));

		if (!veiculoRepositoryPort.buscarPorClienteId(id).isEmpty()) {
			throw new ResourceInUseException("Cliente possui veículos vinculados e não pode ser excluído.");
		}

		if (!osRepositoryPort.buscarPorClienteId(id).isEmpty()) {
			throw new ResourceInUseException("Cliente possui ordens de serviço vinculadas e não pode ser excluído.");
		}

		 clienteRepositoryPort.excluirPorId(id);
		
	}


}
