-- Index supports des contraintes PK / UK, créés explicitement pour être placés dans le tablespace d'index.
CREATE UNIQUE INDEX PK_OST_BUCKET ON OST_BUCKET (ID) ${ostore_index_tablespace_clause};

CREATE UNIQUE INDEX UK_OST_BUCKET_NAME ON OST_BUCKET (NAME) ${ostore_index_tablespace_clause};
