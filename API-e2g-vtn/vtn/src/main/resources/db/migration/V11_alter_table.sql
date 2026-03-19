ALTER TABLE evento_ufsm
    ADD CONSTRAINT unique_evento UNIQUE (id_evento);

ALTER TABLE evento_energy2go
    ADD CONSTRAINT unique_evento_e2g UNIQUE (id_evento);