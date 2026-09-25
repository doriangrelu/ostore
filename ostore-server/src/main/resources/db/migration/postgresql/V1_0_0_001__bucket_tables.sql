-- Buckets : conteneurs logiques d'objets, chacun rattaché à un driver de stockage.
CREATE TABLE OST_BUCKET (
    ID          UUID                     NOT NULL,
    NAME        VARCHAR(63)              NOT NULL,
    DRIVER_ID   VARCHAR(64)              NOT NULL,
    CREATED_AT  TIMESTAMP WITH TIME ZONE NOT NULL,
    VERSION     BIGINT                   NOT NULL
);
