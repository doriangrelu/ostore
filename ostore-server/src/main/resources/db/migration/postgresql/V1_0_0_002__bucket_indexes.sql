-- Index supports des contraintes PK / UK, créés explicitement (convention : un objet par instruction).
CREATE UNIQUE INDEX PK_OST_BUCKET ON OST_BUCKET (ID);

CREATE UNIQUE INDEX UK_OST_BUCKET_NAME ON OST_BUCKET (NAME);
