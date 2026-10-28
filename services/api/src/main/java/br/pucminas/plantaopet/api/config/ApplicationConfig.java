package br.pucminas.plantaopet.api.config;

import br.pucminas.plantaopet.api.service.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

@Configuration
public class ApplicationConfig {

    /** Relógio UTC injetável, para regras dependentes de "agora" serem testáveis. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    IdGenerator idGenerator() {
        return UUID::randomUUID;
    }
}
