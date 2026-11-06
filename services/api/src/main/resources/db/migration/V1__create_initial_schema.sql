-- Schema inicial do PetAmparo.
-- Identificadores UUID são gerados pela aplicação, evitando dependência de extensão específica.

CREATE TABLE tutors (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tutors_email UNIQUE (email)
);

CREATE TABLE caregivers (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_caregivers_email UNIQUE (email)
);

CREATE TABLE pets (
    id UUID PRIMARY KEY,
    tutor_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    species VARCHAR(40) NOT NULL,
    size VARCHAR(20),
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pets_tutor
        FOREIGN KEY (tutor_id)
        REFERENCES tutors (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_pets_size
        CHECK (size IS NULL OR size IN ('SMALL', 'MEDIUM', 'LARGE'))
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    tutor_id UUID NOT NULL,
    pet_id UUID NOT NULL,
    caregiver_id UUID NOT NULL,
    service_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_tutor
        FOREIGN KEY (tutor_id)
        REFERENCES tutors (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_bookings_pet
        FOREIGN KEY (pet_id)
        REFERENCES pets (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT fk_bookings_caregiver
        FOREIGN KEY (caregiver_id)
        REFERENCES caregivers (id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT ck_bookings_service_type
        CHECK (service_type IN (
            'WALK',
            'COMPANIONSHIP',
            'HOME_VISIT',
            'HOSTING',
            'TEMPORARY_CARE'
        )),

    CONSTRAINT ck_bookings_status
        CHECK (status IN (
            'REQUESTED',
            'ACCEPTED',
            'PAID',
            'IN_PROGRESS',
            'COMPLETED',
            'REJECTED',
            'CANCELLED',
            'EXPIRED'
        )),

    CONSTRAINT ck_bookings_period
        CHECK (scheduled_end > scheduled_start)
);
