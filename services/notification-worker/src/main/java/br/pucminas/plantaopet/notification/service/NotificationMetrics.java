package br.pucminas.plantaopet.notification.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métricas de negócio do worker (contrato em docs/architecture/contratos-integracao.md e ADR-005).
 * <ul>
 *   <li>{@code message.processing{event_type, outcome=processed|duplicate}}</li>
 *   <li>{@code message.processing.errors{event_type}} - uma por tentativa com falha</li>
 * </ul>
 */
@Component
public class NotificationMetrics {

    public static final String PROCESSING = "message.processing";
    public static final String PROCESSING_ERRORS = "message.processing.errors";
    public static final String TAG_EVENT_TYPE = "event_type";
    public static final String TAG_OUTCOME = "outcome";
    /** Valor de {@code event_type} quando a mensagem nem pôde ser lida. */
    public static final String UNKNOWN_EVENT_TYPE = "unknown";

    private final MeterRegistry meterRegistry;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void outcome(String eventType, ProcessingOutcome outcome) {
        meterRegistry.counter(PROCESSING, TAG_EVENT_TYPE, eventType, TAG_OUTCOME, outcome.tagValue()).increment();
    }

    public void error(String eventType) {
        meterRegistry.counter(PROCESSING_ERRORS, TAG_EVENT_TYPE, eventType).increment();
    }
}
