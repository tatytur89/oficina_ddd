package br.com.fiap.adapters.out.notification;

import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.out.StatusOsNotification;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.util.HtmlUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StatusOsEmailTemplateTest {
    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void everyStatusHasReadablePlainAndStyledHtmlWithoutPrivateDataOrExternalResources(StatusOS status) {
        var template=new StatusOsEmailTemplate();
        var note=new StatusOsNotification(12345L,status,"private-recipient@test.invalid");
        String plain=template.renderText(note);
        String html=template.renderHtml(note);
        assertThat(plain).contains("12345",status.getDescricao()).doesNotContain("<html",note.destinatario(),"chaveAcesso","JWT");
        String phase="Fase atual: "+(status.ordinal()+1)+"/6";
        assertThat(plain).contains(phase);
        assertThat(HtmlUtils.htmlUnescape(html)).contains("12345",status.getDescricao(),phase).doesNotContain(note.destinatario(),"chaveAcesso","JWT");
        assertThat(html.toLowerCase()).contains("<html","style").doesNotContain("<script","<iframe","<form","http://","https://","url(");
        assertThat(html).doesNotContain("${","{{");
    }
}
