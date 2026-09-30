COMMENT ON TABLE OST_TRANSACTION IS 'Transactions de dépôt : les objets en attente ne sont conservés que si la transaction est validée avant son échéance.';

COMMENT ON COLUMN OST_TRANSACTION.ID IS 'Identifiant de la transaction (UUID v7).';

COMMENT ON COLUMN OST_TRANSACTION.STATUS IS 'OPEN, COMMITTED, ROLLED_BACK ou EXPIRED.';

COMMENT ON COLUMN OST_TRANSACTION.CLIENT_REFERENCE IS 'Référence libre fournie par le client (ex. numéro de dossier).';

COMMENT ON COLUMN OST_TRANSACTION.CREATED_AT IS 'Date d''ouverture (UTC).';

COMMENT ON COLUMN OST_TRANSACTION.EXPIRES_AT IS 'Échéance (UTC) : passée, la transaction est expirée.';

COMMENT ON COLUMN OST_TRANSACTION.CLOSED_AT IS 'Date de clôture (UTC) : validation, annulation ou expiration.';

COMMENT ON COLUMN OST_TRANSACTION.VERSION IS 'Version pour le verrouillage optimiste.';

COMMENT ON COLUMN OST_OBJECT.STATUS IS 'PENDING (en attente de validation) ou ACTIVE (validé, visible dans les listes).';

COMMENT ON COLUMN OST_OBJECT.TRANSACTION_ID IS 'Transaction de l''écriture en attente, ou de la dernière écriture transactionnelle.';

COMMENT ON COLUMN OST_OBJECT.REPLACES_OBJECT_ID IS 'Ligne interne : remplacement en attente de l''objet désigné, appliqué au commit.';