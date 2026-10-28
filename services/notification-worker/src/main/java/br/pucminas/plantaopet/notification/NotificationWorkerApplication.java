package br.pucminas.plantaopet.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Worker que consome eventos de reserva do RabbitMQ e registra/envia (simulado) notificações.
 * Possui banco próprio e nunca acessa o banco da API (ADR-003, ADR-004).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificationWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationWorkerApplication.class, args);
    }
}
