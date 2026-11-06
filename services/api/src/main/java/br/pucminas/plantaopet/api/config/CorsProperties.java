package br.pucminas.plantaopet.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Origens liberadas para CORS ({@code plantaopet.cors.allowed-origins}). */
@ConfigurationProperties(prefix = "plantaopet.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
