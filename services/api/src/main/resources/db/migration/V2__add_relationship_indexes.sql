-- Índices mínimos para colunas de relacionamento.
-- Índices adicionais devem ser justificados pelos contratos e padrões reais de consulta.

CREATE INDEX idx_pets_tutor_id
    ON pets (tutor_id);

CREATE INDEX idx_bookings_tutor_id
    ON bookings (tutor_id);

CREATE INDEX idx_bookings_pet_id
    ON bookings (pet_id);

CREATE INDEX idx_bookings_caregiver_id
    ON bookings (caregiver_id);
