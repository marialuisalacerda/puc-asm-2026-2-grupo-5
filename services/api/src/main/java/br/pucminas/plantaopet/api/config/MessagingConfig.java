package br.pucminas.plantaopet.api.config;

import br.pucminas.plantaopet.api.messaging.BookingEventsTopology;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara a exchange de eventos de reserva (topic, durável). A declaração é idempotente:
 * o notification-worker declara a mesma exchange com os mesmos argumentos, além de fila,
 * bindings e DLQ, que pertencem ao consumidor.
 */
@Configuration
public class MessagingConfig {

    @Bean
    TopicExchange bookingEventsExchange() {
        return ExchangeBuilder.topicExchange(BookingEventsTopology.EXCHANGE).durable(true).build();
    }
}
