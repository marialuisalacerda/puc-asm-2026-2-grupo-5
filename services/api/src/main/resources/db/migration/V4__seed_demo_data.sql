-- Dados de demonstração com IDs fixos, compartilhados com os mocks do aplicativo mobile
-- (docs/architecture/contratos-integracao.md). Dados fictícios: e-mails em example.com,
-- nenhuma credencial ou dado pessoal real.

INSERT INTO tutors (id, name, email) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Ana Tutora',  'ana.tutora@example.com'),
    ('11111111-1111-1111-1111-111111111112', 'Bruno Tutor', 'bruno.tutor@example.com');

INSERT INTO pets (id, tutor_id, name, species, size) VALUES
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 'Thor', 'DOG', 'LARGE'),
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Mel',  'DOG', 'SMALL'),
    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111112', 'Bidu', 'DOG', 'MEDIUM');

INSERT INTO caregivers (id, name, email, active) VALUES
    ('33333333-3333-3333-3333-333333333331', 'Carla Cuidadora', 'carla.cuidadora@example.com', TRUE),
    ('33333333-3333-3333-3333-333333333332', 'Diego Passeador', 'diego.passeador@example.com', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Elisa Anfitriã',  'elisa.anfitria@example.com',  TRUE),
    ('33333333-3333-3333-3333-333333333334', 'Fábio Inativo',   'fabio.inativo@example.com',   FALSE);
