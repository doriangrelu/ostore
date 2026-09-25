COMMENT ON TABLE OST_OBJECT IS 'Objets actifs : clé utilisateur dans un bucket et blob physique associé.';

COMMENT ON COLUMN OST_OBJECT.ID IS 'Identifiant de ressource (UUID v7).';

COMMENT ON COLUMN OST_OBJECT.BUCKET_ID IS 'Bucket contenant l''objet.';

COMMENT ON COLUMN OST_OBJECT.OBJECT_KEY IS 'Clé de l''objet dans le bucket (1 à 1024 octets UTF-8).';

COMMENT ON COLUMN OST_OBJECT.DRIVER_ID IS 'Instance de driver stockant le blob.';

COMMENT ON COLUMN OST_OBJECT.BLOB_PATH IS 'Chemin du blob sous la racine du driver, calculé à l''écriture par la stratégie de chemins (ex. 2026/09/25/<uuid>).';

COMMENT ON COLUMN OST_OBJECT.SIZE_BYTES IS 'Taille du contenu en octets.';

COMMENT ON COLUMN OST_OBJECT.ETAG IS 'Empreinte MD5 hexadécimale du contenu.';

COMMENT ON COLUMN OST_OBJECT.CONTENT_TYPE IS 'Type MIME fourni au dépôt.';

COMMENT ON COLUMN OST_OBJECT.CREATED_AT IS 'Date de dépôt (UTC).';

COMMENT ON COLUMN OST_OBJECT.VERSION IS 'Version pour le verrouillage optimiste.';

COMMENT ON TABLE OST_OBJECT_METADATA IS 'Métadonnées utilisateur des objets.';

COMMENT ON COLUMN OST_OBJECT_METADATA.NAME IS 'Nom de la métadonnée (minuscules).';

COMMENT ON COLUMN OST_OBJECT_METADATA.VALUE IS 'Valeur de la métadonnée.';

COMMENT ON TABLE OST_BLOB_PURGE IS 'Blobs à supprimer physiquement, traités par le job de purge.';

COMMENT ON COLUMN OST_BLOB_PURGE.NOT_BEFORE IS 'Date (UTC) à partir de laquelle la purge peut être tentée.';

COMMENT ON COLUMN OST_BLOB_PURGE.ATTEMPTS IS 'Nombre de tentatives échouées.';

COMMENT ON COLUMN OST_BLOB_PURGE.LAST_ERROR IS 'Dernière erreur rencontrée.';