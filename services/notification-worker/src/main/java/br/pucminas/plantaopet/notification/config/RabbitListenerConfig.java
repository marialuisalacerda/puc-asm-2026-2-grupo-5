package br.pucminas.plantaopet.notification.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

/**
 * Container do listener com retentativa local (stateless) e dead-lettering.
 *
 * <ul>
 *   <li>Usa os valores de {@code spring.rabbitmq.listener.simple.retry.*} (3 tentativas, 1s, 2s).</li>
 *   <li>{@link AmqpRejectAndDontRequeueException} (mensagem inválida / eventType desconhecido) <b>não</b>
 *       é retentada: vai direto para a DLQ.</li>
 *   <li>Esgotadas as tentativas, {@link RejectAndDontRequeueRecoverer} rejeita sem requeue e o broker
 *       encaminha para {@code booking.events.dlx} e dali para {@code notification.booking-events.dlq}.</li>
 * </ul>
 */
@Configuration
public class RabbitListenerConfig {

    @Bean
    public RetryOperationsInterceptor bookingEventsRetryInterceptor(RabbitProperties rabbitProperties) {
        RabbitProperties.ListenerRetry retry = rabbitProperties.getListener().getSimple().getRetry();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                retry.getMaxAttempts(),
                Map.of(AmqpRejectAndDontRequeueException.class, false),
                true,   // inspeciona as causas (o container embrulha em ListenerExecutionFailedException)
                true);  // demais exceções são retentáveis

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(retry.getInitialInterval().toMillis());
        backOff.setMultiplier(retry.getMultiplier());
        backOff.setMaxInterval(retry.getMaxInterval().toMillis());

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOff);

        return RetryInterceptorBuilder.stateless()
                .retryOperations(retryTemplate)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            RabbitProperties rabbitProperties,
            RetryOperationsInterceptor bookingEventsRetryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        // substitui a cadeia padrão do Boot para classificar exceções não retentáveis
        if (rabbitProperties.getListener().getSimple().getRetry().isEnabled()) {
            factory.setAdviceChain(bookingEventsRetryInterceptor);
        }
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
