package br.com.fiap.adapters.out.notification;

import br.com.fiap.domain.valueobjects.StatusOS;
import br.com.fiap.ports.out.StatusOsNotification;
import br.com.fiap.ports.out.StatusOsNotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailStatusNotificationConfigurationTest {
    private final ApplicationContextRunner runner=new ApplicationContextRunner()
            .withUserConfiguration(EmailStatusNotificationConfiguration.class);

    @Test
    void missingFlagProvidesOnlyANoopPortWithoutAMailSender() {
        runner.withInitializer(context->context.getEnvironment().getPropertySources().remove("systemEnvironment"))
                .run(context->{
                    assertThat(context).hasNotFailed().hasSingleBean(StatusOsNotificationPort.class)
                            .doesNotHaveBean(SmtpStatusOsNotificationListener.class).doesNotHaveBean(JavaMailSender.class);
                    context.getBean(StatusOsNotificationPort.class).notificar(new StatusOsNotification(42L,StatusOS.RECEBIDA,"cliente@test.invalid"));
                });
    }

    @Test
    void disabledEmailNeverCallsAnAvailableSender() {
        var sender=mock(JavaMailSender.class);
        runner.withBean(JavaMailSender.class,()->sender).withPropertyValues("app.notifications.email.enabled=false")
                .run(context->{
                    assertThat(context).hasNotFailed().hasSingleBean(StatusOsNotificationPort.class).doesNotHaveBean(SmtpStatusOsNotificationListener.class);
                    context.getBean(StatusOsNotificationPort.class).notificar(new StatusOsNotification(42L,StatusOS.RECEBIDA,"cliente@test.invalid"));
                    verifyNoInteractions(sender);
                });
    }

    @Test
    void enabledEmailRequiresAnExternalPassword() {
        runner.withBean(JavaMailSender.class,()->mock(JavaMailSender.class))
                .withPropertyValues("app.notifications.email.enabled=true","spring.mail.username=sender@test.invalid","spring.mail.password=","app.notifications.email.from=sender@test.invalid")
                .run(context->{
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class).hasStackTraceContaining("SMTP_PASSWORD");
                });
    }

    @Test
    void enabledEmailRejectsBlankUsername() {
        runner.withBean(JavaMailSender.class,()->mock(JavaMailSender.class))
                .withPropertyValues("app.notifications.email.enabled=true","spring.mail.username=","spring.mail.password=fake-test-password","app.notifications.email.from=sender@test.invalid")
                .run(context->{
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class).hasStackTraceContaining("SMTP_USERNAME");
                });
    }

    @Test
    void enabledEmailRejectsBlankSender() {
        runner.withBean(JavaMailSender.class,()->mock(JavaMailSender.class))
                .withPropertyValues("app.notifications.email.enabled=true","spring.mail.username=sender@test.invalid","spring.mail.password=fake-test-password","app.notifications.email.from=")
                .run(context->{
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                });
    }

    @Test
    void configuredSenderUsesLoginTlsIdentityChecksAndBoundedTimeoutsWithoutConnecting() throws Exception {
        var autoConfiguration=Class.forName("org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration");
        var mailHealthConfiguration=Class.forName("org.springframework.boot.mail.autoconfigure.MailHealthContributorAutoConfiguration");
        runner.withConfiguration(AutoConfigurations.of(autoConfiguration,mailHealthConfiguration))
                .withInitializer(context->{
                    try{context.getEnvironment().getPropertySources().addLast(new ResourcePropertySource("production-mail-properties",new FileSystemResource("src/main/resources/application.properties")));}
                    catch(IOException exception){throw new UncheckedIOException(exception);}
                })
                .withPropertyValues("app.notifications.email.enabled=true","spring.mail.host=127.0.0.1","spring.mail.port=2525",
                        "spring.mail.username=sender@test.invalid","spring.mail.password=fake-test-password","app.notifications.email.from=sender@test.invalid")
                .run(context->{
                    assertThat(context).hasNotFailed().hasSingleBean(StatusOsNotificationPort.class).hasSingleBean(SmtpStatusOsNotificationListener.class);
                    assertThat(context.getEnvironment().getProperty("management.health.mail.enabled",Boolean.class)).isFalse();
                    assertThat(context).doesNotHaveBean("mailHealthContributor").doesNotHaveBean("mailHealthIndicator");
                    var sender=(JavaMailSenderImpl)context.getBean(JavaMailSender.class);
                    assertThat(sender.getHost()).isEqualTo("127.0.0.1");
                    assertThat(sender.getProtocol()).isEqualTo("smtps");
                    assertThat(sender.getDefaultEncoding()).isEqualToIgnoringCase("UTF-8");
                    for(String protocol:new String[]{"smtp","smtps"}){
                        String prefix="mail."+protocol+".";
                        assertThat(sender.getJavaMailProperties().getProperty(prefix+"auth")).isEqualTo("true");
                        assertThat(sender.getJavaMailProperties().getProperty(prefix+"auth.mechanisms")).isEqualTo("LOGIN");
                        assertThat(sender.getJavaMailProperties().getProperty(prefix+"ssl.enable")).isEqualTo("true");
                        assertThat(sender.getJavaMailProperties().getProperty(prefix+"ssl.checkserveridentity")).isEqualTo("true");
                        for(String timeout:new String[]{"connectiontimeout","timeout","writetimeout"}){
                            assertThat(sender.getJavaMailProperties().getProperty(prefix+timeout)).isEqualTo("5000");
                        }
                    }
                });
    }
}
