-- Objets : une ligne par objet actif, pointant vers son blob physique (chemin interne, jamais la clé utilisateur).
-- OBJECT_KEY en octets : la clé est limitée à 1024 octets UTF-8. Tri binaire garanti par la session (NLS_SORT=BINARY).
CREATE TABLE OST_OBJECT (
    ID            RAW(16)                  NOT NULL,
    BUCKET_ID     RAW(16)                  NOT NULL,
    OBJECT_KEY    VARCHAR2(1024 BYTE)      NOT NULL,
    DRIVER_ID     VARCHAR2(64 CHAR)        NOT NULL,
    BLOB_PATH     VARCHAR2(512 CHAR)       NOT NULL,
    SIZE_BYTES    NUMBER(19)               NOT NULL,
    ETAG          VARCHAR2(64 CHAR)        NOT NULL,
    CONTENT_TYPE  VARCHAR2(255 CHAR)       NOT NULL,
    CREATED_AT    TIMESTAMP WITH TIME ZONE NOT NULL,
    VERSION       NUMBER(19)               NOT NULL
) ${ostore_table_tablespace_clause};

-- Métadonnées utilisateur (en-têtes X-OStore-Meta).
CREATE TABLE OST_OBJECT_METADATA (
    OBJECT_ID  RAW(16)             NOT NULL,
    NAME       VARCHAR2(64 CHAR)   NOT NULL,
    VALUE      VARCHAR2(2048 CHAR) NOT NULL
) ${ostore_table_tablespace_clause};

-- File des blobs à supprimer physiquement (remplacement, suppression, échec d'écriture).
CREATE TABLE OST_BLOB_PURGE (
    ID          NUMBER(19) GENERATED ALWAYS AS IDENTITY,
    DRIVER_ID   VARCHAR2(64 CHAR)        NOT NULL,
    BLOB_PATH   VARCHAR2(512 CHAR)       NOT NULL,
    NOT_BEFORE  TIMESTAMP WITH TIME ZONE NOT NULL,
    ATTEMPTS    NUMBER(10)               NOT NULL,
    LAST_ERROR  VARCHAR2(1000 CHAR),
    CREATED_AT  TIMESTAMP WITH TIME ZONE NOT NULL
) ${ostore_table_tablespace_clause};