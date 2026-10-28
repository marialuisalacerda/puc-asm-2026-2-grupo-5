package br.pucminas.plantaopet.notification.service.sender;

import br.pucminas.plantaopet.notification.domain.Notification;
import br.pucminas.plantaopet.notification.domain.NotificationChannel;

/**
 * Porta de saída para o canal de entrega. A implementação atual apenas registra em log; um
 * fornecedor real (e-mail, push) entra como outra implementação sem mudar o processamento.
 */
public interface NotificationSender {

    NotificationChannel channel();

    void send(Notification notification);
}
