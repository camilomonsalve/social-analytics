CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE profile
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(255) NOT NULL,
    descripcion TEXT,
    foto        VARCHAR(1024),
    categoria   VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_profile_categoria ON profile (categoria);
