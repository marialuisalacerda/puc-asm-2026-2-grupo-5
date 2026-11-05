package br.pucminas.plantaopet.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara a topologia do worker. O RabbitAdmin auto-configurado aplica as declarações ao conectar
 * (idempotente: declarar de novo com os mesmos argumentos não altera nada).
 *
 * <pre>
 * booking.events (topic) --booking.*--&gt; notification.booking-events --(rejeitada)--&gt; booking.events.dlx (fanout)
 *                                                                                    --&gt; notification.booking-events.dlq
 * </pre>
 */
@Configuration
public class RabbitTopologyConfig {

    @Bean
    public Declarables bookingEventsTopology(MessagingProperties props) {
        TopicExchange exchange = ExchangeBuilder.topicExchange(props.exchange()).durable(true).build();
        FanoutExchange deadLetterExchange = ExchangeBuilder.fanoutExchange(props.deadLetterExchange())
                .durable(true).build();

        Queue queue = QueueBuilder.durable(props.queue())
                .deadLetterExchange(props.deadLetterExchange())
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(props.deadLetterQueue()).build();

        Binding queueBinding = BindingBuilder.bind(queue).to(exchange).with(props.bindingKey());
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange);

        return new Declarables(exchange, deadLetterExchange, queue, deadLetterQueue, queueBinding, deadLetterBinding);
    }
}
