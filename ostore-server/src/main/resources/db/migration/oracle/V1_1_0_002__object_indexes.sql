CREATE UNIQUE INDEX PK_OST_OBJECT ON OST_OBJECT (ID) ${ostore_index_tablespace_clause};

-- Unicité d'une clé dans un bucket ; sert aussi l'index de la FK BUCKET_ID et la pagination par clé.
CREATE UNIQUE INDEX UK_OST_OBJECT_BUCKET_KEY ON OST_OBJECT (BUCKET_ID, OBJECT_KEY) ${ostore_index_tablespace_clause};

-- Colonne de tête = FK OBJECT_ID.
CREATE UNIQUE INDEX PK_OST_OBJECT_METADATA ON OST_OBJECT_METADATA (OBJECT_ID, NAME) ${ostore_index_tablespace_clause};

CREATE UNIQUE INDEX PK_OST_BLOB_PURGE ON OST_BLOB_PURGE (ID) ${ostore_index_tablespace_clause};

-- Sélection des blobs échus par le job de purge.
CREATE INDEX IX_OST_BLOB_PURGE_NOT_BEFORE ON OST_BLOB_PURGE (NOT_BEFORE) ${ostore_index_tablespace_clause};