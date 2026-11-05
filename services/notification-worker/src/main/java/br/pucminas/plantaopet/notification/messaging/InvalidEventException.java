package br.pucminas.plantaopet.notification.messaging;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * Mensagem que nunca poderá ser processada (JSON malformado, envelope inválido, versão ou
 * eventType desconhecidos). Não é retentada: vai direto para a DLQ.
 */
public class InvalidEventException extends AmqpRejectAndDontRequeueException {

    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
