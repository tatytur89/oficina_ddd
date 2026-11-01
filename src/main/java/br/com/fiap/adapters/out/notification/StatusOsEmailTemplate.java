package br.com.fiap.adapters.out.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.HtmlUtils;

import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.out.StatusOsNotification;

public class StatusOsEmailTemplate {

    public String renderText(StatusOsNotification notification) {
        return "OFICINA FIAP\nAtualização do atendimento\n\nOS #" + notification.osId()
                + "\nStatus: " + notification.status().getDescricao()
                + "\nFase atual: " + etapa(notification.status()) + "/6\n\n"
                + mensagem(notification.status())
                + "\n\nPara esclarecer dúvidas sobre o atendimento, fale com a equipe da oficina.";
    }

    public String renderHtml(StatusOsNotification notification) {
        String html;
        try {
            html = new ClassPathResource("templates/mail/status-os.html")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível carregar o modelo de notificação.");
        }
        int etapaAtual = etapa(notification.status());
        var valores = new LinkedHashMap<String, String>();
        valores.put("OS_ID", String.valueOf(notification.osId()));
        valores.put("STATUS", notification.status().getDescricao());
        valores.put("CURRENT_STEP", String.valueOf(etapaAtual));
        valores.put("MESSAGE", mensagem(notification.status()));
        for (int etapa = 1; etapa <= 6; etapa++) {
            valores.put("STEP_" + etapa + "_BACKGROUND",
                    etapa < etapaAtual ? "#2b5cab" : etapa == etapaAtual ? "#e49b42" : "#dbe3e9");
            valores.put("STEP_" + etapa + "_COLOR", etapa < etapaAtual ? "#ffffff" : "#142638");
        }
        for (var valor : valores.entrySet()) {
            html = html.replace("{{" + valor.getKey() + "}}", HtmlUtils.htmlEscape(valor.getValue()));
        }
        return html;
    }

    private int etapa(StatusOS status) {
        return switch (status) {
            case RECEBIDA -> 1;
            case EM_DIAGNOSTICO -> 2;
            case AGUARDANDO_APROVACAO -> 3;
            case EM_EXECUCAO -> 4;
            case FINALIZADA -> 5;
            case ENTREGUE -> 6;
        };
    }

    private String mensagem(StatusOS status) {
        return switch (status) {
            case RECEBIDA -> "Recebemos sua ordem de serviço. A próxima etapa é o diagnóstico do veículo.";
            case EM_DIAGNOSTICO -> "Nossa equipe está analisando o veículo e preparando os próximos passos do atendimento.";
            case AGUARDANDO_APROVACAO -> "O orçamento está disponível e aguardamos sua decisão para continuar o atendimento.";
            case EM_EXECUCAO -> "O orçamento foi aprovado e nossa equipe está executando os serviços previstos.";
            case FINALIZADA -> "Os serviços foram concluídos. Entre em contato com a equipe para combinar a retirada do veículo.";
            case ENTREGUE -> "Seu veículo foi entregue. Agradecemos a confiança em nossa oficina.";
        };
    }
}
