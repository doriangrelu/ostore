CREATE UNIQUE INDEX PK_OST_TRANSACTION ON OST_TRANSACTION (ID) ${ostore_index_tablespace_clause};

-- Sélection des transactions échues par le job d'expiration.
CREATE INDEX IX_OST_TRANSACTION_EXPIRY ON OST_TRANSACTION (STATUS, EXPIRES_AT) ${ostore_index_tablespace_clause};

-- La liste d'un bucket ne montre que les objets validés : le statut rejoint l'index de pagination
-- (colonne de tête BUCKET_ID : sert toujours la FK).
DROP INDEX IX_OST_OBJECT_BUCKET_ID;

CREATE INDEX IX_OST_OBJECT_BUCKET_STATUS ON OST_OBJECT (BUCKET_ID, STATUS, ID) ${ostore_index_tablespace_clause};

-- FK TRANSACTION_ID et liste des objets d'une transaction.
CREATE INDEX IX_OST_OBJECT_TRANSACTION ON OST_OBJECT (TRANSACTION_ID, ID) ${ostore_index_tablespace_clause};

-- Un seul remplacement en attente par objet (les NULL ne sont pas indexés) ; sert aussi la FK.
CREATE UNIQUE INDEX UK_OST_OBJECT_REPLACES ON OST_OBJECT (REPLACES_OBJECT_ID) ${ostore_index_tablespace_clause};