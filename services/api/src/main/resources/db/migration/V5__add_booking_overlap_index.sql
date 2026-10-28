-- Índice para a verificação de sobreposição executada a cada POST /bookings
-- (regra 3 do contrato de integração):
--
--   WHERE caregiver_id = ? AND status IN ('REQUESTED', 'ACCEPTED')
--     AND scheduled_start < :fim AND scheduled_end > :inicio
--
-- A ordem (igualdade em caregiver_id, filtro em status, faixa em scheduled_start)
-- permite ao PostgreSQL resolver a consulta com um index range scan, sem varrer
-- todo o histórico de reservas do cuidador. Também atende GET /bookings?caregiverId=&status=.

CREATE INDEX idx_bookings_caregiver_status_start
    ON bookings (caregiver_id, status, scheduled_start);
