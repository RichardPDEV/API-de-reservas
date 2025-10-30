-- Extensión para EXCLUDE con ranges
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Negocio
CREATE TABLE business (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Recurso reservable (mesa/sala)
CREATE TABLE resource (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    capacity INT NOT NULL CHECK (capacity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Política de cancelación por negocio
CREATE TABLE cancellation_policy (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    free_before_minutes INT NOT NULL CHECK (free_before_minutes >= 0),
    penalty_type TEXT NOT NULL, -- e.g., NOTE, FEE
    penalty_amount NUMERIC(10, 2) NOT NULL DEFAULT 0
);

-- Reservas
CREATE TABLE reservation (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resource (id) ON DELETE CASCADE,
    customer_name TEXT NOT NULL,
    customer_email TEXT NOT NULL,
    party_size INT NOT NULL CHECK (party_size > 0),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, CANCELLED, LATE_CANCELLED
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (start_time < end_time)
);

-- Índices útiles para consultas por recurso + tiempo
CREATE INDEX idx_reservation_resource_time ON reservation (resource_id, start_time);

-- Constraint anti-solape por recurso usando tstzrange
ALTER TABLE reservation
ADD CONSTRAINT uq_reservation_no_overlap EXCLUDE USING gist (
    resource_id
    WITH
        =,
        tstzrange (start_time, end_time, '[]')
    WITH
        &&
)
WHERE (status = 'CONFIRMED');