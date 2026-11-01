package br.com.fiap.ports.out;

import br.com.fiap.domain.valueobjects.StatusOS;

public record StatusOsNotification(Long osId, StatusOS status, String destinatario) {
}
