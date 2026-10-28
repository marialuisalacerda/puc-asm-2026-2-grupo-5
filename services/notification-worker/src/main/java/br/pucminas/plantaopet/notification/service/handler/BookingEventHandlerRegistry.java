package br.pucminas.plantaopet.notification.service.handler;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Seleciona a estratégia ({@link BookingEventHandler}) pelo {@code eventType}. */
@Component
public class BookingEventHandlerRegistry {

    private final Map<String, BookingEventHandler> handlers;

    public BookingEventHandlerRegistry(List<BookingEventHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(BookingEventHandler::eventType, Function.identity()));
    }

    public Optional<BookingEventHandler> find(String eventType) {
        return Optional.ofNullable(eventType).map(handlers::get);
    }
}
