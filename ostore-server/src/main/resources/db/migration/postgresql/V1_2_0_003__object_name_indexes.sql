-- Index de la FK BUCKET_ID et de la liste d'un bucket paginée par identifiant (UUID v7 = ordre de dépôt).
CREATE INDEX IX_OST_OBJECT_BUCKET_ID ON OST_OBJECT (BUCKET_ID, ID);

-- Filtre de liste par préfixe de nom.
CREATE INDEX IX_OST_OBJECT_BUCKET_NAME ON OST_OBJECT (BUCKET_ID, OBJECT_NAME);