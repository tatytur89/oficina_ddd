package br.com.fiap.adapters.out.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import br.com.fiap.ports.out.StatusOsNotificationPort;

@Configuration(proxyBeanMethods = false)
public class EmailStatusNotificationConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "false", matchIfMissing = true)
    StatusOsNotificationPort disabledStatusOsNotifications() {
        return notification -> { };
    }

    @Bean
    @ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
    StatusOsNotificationPort enabledStatusOsNotifications(ApplicationEventPublisher events,
            @Value("${spring.mail.username:projetofiapx@gmail.com}") String username,
            @Value("${spring.mail.password:}") String password) {
        if (password.isBlank()) {
            throw new IllegalStateException("SMTP_PASSWORD é obrigatório quando as notificações por e-mail estão habilitadas.");
        }
        if (username.isBlank()) {
            throw new IllegalStateException("SMTP_USERNAME é obrigatório quando as notificações por e-mail estão habilitadas.");
        }
        return new TransactionalStatusOsNotificationPublisher(events);
    }

    @Bean
    @ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
    SmtpStatusOsNotificationListener statusOsEmailListener(JavaMailSender sender,
            @Value("${app.notifications.email.from:projetofiapx@gmail.com}") String from) {
        return new SmtpStatusOsNotificationListener(sender, from);
    }
}
