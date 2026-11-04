-- Preço da reserva calculado pela API na criação (Strategy por tipo de serviço).
-- Valor inteiro em centavos de BRL, evitando arredondamento de ponto flutuante.
-- DEFAULT 0 mantém compatibilidade com linhas existentes antes desta migration.

ALTER TABLE bookings
    ADD COLUMN price_cents BIGINT NOT NULL DEFAULT 0;

ALTER TABLE bookings
    ADD CONSTRAINT ck_bookings_price_cents
        CHECK (price_cents >= 0);
