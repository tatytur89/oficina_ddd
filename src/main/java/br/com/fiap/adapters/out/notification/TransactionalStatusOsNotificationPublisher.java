package br.com.fiap.adapters.out.notification;

import org.springframework.context.ApplicationEventPublisher;

import br.com.fiap.ports.out.StatusOsNotification;
import br.com.fiap.ports.out.StatusOsNotificationPort;

public class TransactionalStatusOsNotificationPublisher implements StatusOsNotificationPort {

    private final ApplicationEventPublisher events;

    public TransactionalStatusOsNotificationPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void notificar(StatusOsNotification notification) {
        events.publishEvent(notification);
    }
}
