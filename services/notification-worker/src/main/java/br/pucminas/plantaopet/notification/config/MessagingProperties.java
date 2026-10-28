package br.pucminas.plantaopet.notification.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Nomes da topologia RabbitMQ (contrato em docs/api/eventos.md). */
@Validated
@ConfigurationProperties(prefix = "notification.messaging")
public record MessagingProperties(
        @NotBlank String exchange,
        @NotBlank String queue,
        @NotBlank String bindingKey,
        @NotBlank String deadLetterExchange,
        @NotBlank String deadLetterQueue) {
}
