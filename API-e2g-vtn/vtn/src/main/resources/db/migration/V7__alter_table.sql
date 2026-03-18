ALTER TABLE evento ADD COLUMN chave VARCHAR(255);

ALTER TABLE evento ADD CONSTRAINT uk_evento_chave UNIQUE (chave);