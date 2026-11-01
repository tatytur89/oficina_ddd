package br.com.fiap.adapters.out.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.com.fiap.ports.out.StatusOsNotification;

import jakarta.mail.MessagingException;

public class SmtpStatusOsNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpStatusOsNotificationListener.class);

    private final JavaMailSender sender;
    private final String from;
    private final StatusOsEmailTemplate template = new StatusOsEmailTemplate();

    public SmtpStatusOsNotificationListener(JavaMailSender sender, String from) {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("O remetente SMTP é obrigatório quando as notificações estão habilitadas.");
        }
        this.sender = sender;
        this.from = from;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void onStatusChanged(StatusOsNotification notification) {
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setValidateAddresses(true);
            helper.setFrom(from);
            helper.setTo(notification.destinatario());
            helper.setSubject("Atualização da OS #" + notification.osId());
            helper.setText(template.renderText(notification), template.renderHtml(notification));
            sender.send(message);
            LOG.info("status_os_email_sent osId={} status={}", notification.osId(), notification.status());
        } catch (MessagingException | RuntimeException exception) {
            // Melhor esforço após commit: não há outbox nem repetição automática.
            // Não registrar exceção, destinatário ou conteúdo: falhas SMTP podem conter dados privados.
            LOG.warn("status_os_email_failed osId={} status={}", notification.osId(), notification.status());
        }
    }
}
