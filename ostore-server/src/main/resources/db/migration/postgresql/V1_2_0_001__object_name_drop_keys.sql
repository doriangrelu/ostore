-- La clé n'identifie plus l'objet : l'unicité (bucket, clé) disparaît (l'index support est supprimé avec elle).
ALTER TABLE OST_OBJECT DROP CONSTRAINT UK_OST_OBJECT_BUCKET_KEY;