CREATE UNIQUE INDEX PK_OST_TRANSACTION ON OST_TRANSACTION (ID);

-- Sélection des transactions échues par le job d'expiration.
CREATE INDEX IX_OST_TRANSACTION_EXPIRY ON OST_TRANSACTION (STATUS, EXPIRES_AT);

-- La liste d'un bucket ne montre que les objets validés : le statut rejoint l'index de pagination
-- (colonne de tête BUCKET_ID : sert toujours la FK).
DROP INDEX IX_OST_OBJECT_BUCKET_ID;

CREATE INDEX IX_OST_OBJECT_BUCKET_STATUS ON OST_OBJECT (BUCKET_ID, STATUS, ID);

-- FK TRANSACTION_ID et liste des objets d'une transaction.
CREATE INDEX IX_OST_OBJECT_TRANSACTION ON OST_OBJECT (TRANSACTION_ID, ID);

-- Un seul remplacement en attente par objet (les NULL ne sont pas concernés) ; sert aussi la FK.
CREATE UNIQUE INDEX UK_OST_OBJECT_REPLACES ON OST_OBJECT (REPLACES_OBJECT_ID);