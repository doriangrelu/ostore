-- La clé n'identifie plus l'objet : l'unicité (bucket, clé) disparaît. L'index support ayant été créé à part,
-- Oracle le conserve : il est supprimé explicitement.
ALTER TABLE OST_OBJECT DROP CONSTRAINT UK_OST_OBJECT_BUCKET_KEY;

DROP INDEX UK_OST_OBJECT_BUCKET_KEY;