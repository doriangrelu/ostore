-- Buckets : conteneurs logiques d'objets, chacun rattaché à un driver de stockage.
CREATE TABLE OST_BUCKET (
    ID          RAW(16)                  NOT NULL,
    NAME        VARCHAR2(63 CHAR)        NOT NULL,
    DRIVER_ID   VARCHAR2(64 CHAR)        NOT NULL,
    CREATED_AT  TIMESTAMP WITH TIME ZONE NOT NULL,
    VERSION     NUMBER(19)               NOT NULL
) ${ostore_table_tablespace_clause};
