-- Objets : une ligne par objet actif, pointant vers son blob physique (chemin interne, jamais la clé utilisateur).
-- OBJECT_KEY en collation "C" : tri binaire UTF-8, identique à Oracle (NLS_SORT=BINARY), requis par la pagination.
CREATE TABLE OST_OBJECT (
    ID            UUID                     NOT NULL,
    BUCKET_ID     UUID                     NOT NULL,
    OBJECT_KEY    VARCHAR(1024) COLLATE "C" NOT NULL,
    DRIVER_ID     VARCHAR(64)              NOT NULL,
    BLOB_PATH     VARCHAR(512)             NOT NULL,
    SIZE_BYTES    BIGINT                   NOT NULL,
    ETAG          VARCHAR(64)              NOT NULL,
    CONTENT_TYPE  VARCHAR(255)             NOT NULL,
    CREATED_AT    TIMESTAMP WITH TIME ZONE NOT NULL,
    VERSION       BIGINT                   NOT NULL
);

-- Métadonnées utilisateur (en-têtes X-OStore-Meta).
CREATE TABLE OST_OBJECT_METADATA (
    OBJECT_ID  UUID          NOT NULL,
    NAME       VARCHAR(64)   NOT NULL,
    VALUE      VARCHAR(2048) NOT NULL
);

-- File des blobs à supprimer physiquement (remplacement, suppression, échec d'écriture).
CREATE TABLE OST_BLOB_PURGE (
    ID          BIGINT GENERATED ALWAYS AS IDENTITY,
    DRIVER_ID   VARCHAR(64)              NOT NULL,
    BLOB_PATH   VARCHAR(512)             NOT NULL,
    NOT_BEFORE  TIMESTAMP WITH TIME ZONE NOT NULL,
    ATTEMPTS    INTEGER                  NOT NULL,
    LAST_ERROR  VARCHAR(1000),
    CREATED_AT  TIMESTAMP WITH TIME ZONE NOT NULL
);