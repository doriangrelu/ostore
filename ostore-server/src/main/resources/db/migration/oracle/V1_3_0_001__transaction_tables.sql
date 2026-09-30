-- Transactions : un dépôt en attente n'est conservé que si la transaction est validée avant son échéance.
CREATE TABLE OST_TRANSACTION (
    ID                RAW(16)                  NOT NULL,
    STATUS            VARCHAR2(16 CHAR)        NOT NULL,
    CLIENT_REFERENCE  VARCHAR2(255 CHAR),
    CREATED_AT        TIMESTAMP WITH TIME ZONE NOT NULL,
    EXPIRES_AT        TIMESTAMP WITH TIME ZONE NOT NULL,
    CLOSED_AT         TIMESTAMP WITH TIME ZONE,
    VERSION           NUMBER(19)               NOT NULL
) ${ostore_table_tablespace_clause};

-- Les objets existants sont validés.
ALTER TABLE OST_OBJECT ADD (STATUS VARCHAR2(16 CHAR));

UPDATE OST_OBJECT SET STATUS = 'ACTIVE';

ALTER TABLE OST_OBJECT MODIFY (STATUS NOT NULL);

ALTER TABLE OST_OBJECT ADD (TRANSACTION_ID RAW(16));

ALTER TABLE OST_OBJECT ADD (REPLACES_OBJECT_ID RAW(16));