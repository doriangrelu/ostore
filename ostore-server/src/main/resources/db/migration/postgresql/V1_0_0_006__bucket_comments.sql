COMMENT ON TABLE OST_BUCKET IS 'Buckets : conteneurs logiques d''objets (compatibles S3).';

COMMENT ON COLUMN OST_BUCKET.ID IS 'Identifiant technique (UUID v7).';

COMMENT ON COLUMN OST_BUCKET.NAME IS 'Nom du bucket, unique, conforme aux règles de nommage S3.';

COMMENT ON COLUMN OST_BUCKET.DRIVER_ID IS 'Identifiant du driver de stockage physique des objets du bucket.';

COMMENT ON COLUMN OST_BUCKET.CREATED_AT IS 'Date de création.';

COMMENT ON COLUMN OST_BUCKET.VERSION IS 'Version pour le verrouillage optimiste.';
