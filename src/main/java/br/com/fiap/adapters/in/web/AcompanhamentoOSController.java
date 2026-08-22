package br.com.fiap.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import br.com.fiap.application.exceptions.ResourceNotFoundException;
import br.com.fiap.domain.entities.OrdemServico;
import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.in.OrdemServicoUseCase;

@Controller
public class AcompanhamentoOSController {

    private final OrdemServicoUseCase osUseCase;

    public AcompanhamentoOSController(OrdemServicoUseCase osUseCase) {
        this.osUseCase = osUseCase;
    }

    @GetMapping("/acompanhamento/{id}")
    public String acompanhar(@PathVariable Long id, @RequestParam String chave, Model model) {
        OrdemServico os = osUseCase.buscarParaAcompanhamento(id, chave);
        preencherModel(model, os, chave);
        return "acompanhamento";
    }

    @PostMapping("/acompanhamento/{id}/aprovar")
    public String aprovar(@PathVariable Long id, @RequestParam String chave) {
        osUseCase.aprovarOrcamento(id, chave);
        return "redirect:/acompanhamento/" + id + "?chave=" + chave;
    }

    @PostMapping("/acompanhamento/{id}/avaliar")
    public String avaliar(@PathVariable Long id, @RequestParam String chave,
                           @RequestParam int nota, @RequestParam(required = false) String comentario) {
        osUseCase.avaliarServico(id, chave, nota, comentario);
        return "redirect:/acompanhamento/" + id + "?chave=" + chave;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String tratarNaoEncontrada(RuntimeException ex, Model model) {
        model.addAttribute("erro", ex.getMessage());
        return "acompanhamento-erro";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String tratarChaveOuEstadoInvalido(RuntimeException ex, Model model) {
        model.addAttribute("erro", ex.getMessage());
        return "acompanhamento-erro";
    }

    private void preencherModel(Model model, OrdemServico os, String chave) {
        model.addAttribute("id", os.getId());
        model.addAttribute("status", os.getStatus().name());
        model.addAttribute("statusDescricao", os.getStatus().getDescricao());
        model.addAttribute("dataAbertura", os.getDataAbertura());
        model.addAttribute("dataPrevistaEntrega", os.getDataPrevistaEntrega());
        model.addAttribute("dataConclusao", os.getDataConclusao());
        model.addAttribute("observacoes", os.getObservacoes());
        model.addAttribute("valorServicos", os.getValorServicos().getValor());
        model.addAttribute("valorPecas", os.getValorPecas().getValor());
        model.addAttribute("valorTotal", os.getValorTotal().getValor());
        model.addAttribute("servicos", os.getServicos());
        model.addAttribute("pecas", os.getPecas());
        model.addAttribute("chave", chave);
        model.addAttribute("podeAprovar", os.getStatus() == StatusOS.AGUARDANDO_APROVACAO);
        model.addAttribute("podeAvaliar", os.getStatus() == StatusOS.ENTREGUE && os.getNotaAvaliacao() == null);
        model.addAttribute("notaAvaliacao", os.getNotaAvaliacao());
        model.addAttribute("comentarioAvaliacao", os.getComentarioAvaliacao());
    }
}
