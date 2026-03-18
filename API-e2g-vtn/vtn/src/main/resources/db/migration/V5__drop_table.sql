drop table inversor_evento;

CREATE TABLE evento_ufsm (
                             id SERIAL PRIMARY KEY,

                             id_evento INTEGER NOT NULL,
                             id_api VARCHAR(100),

                             id_inversor INTEGER NOT NULL,

                             data_inicial TIMESTAMP NOT NULL,
                             data_final TIMESTAMP NOT NULL,
                             potencia DOUBLE PRECISION,

                             CONSTRAINT fk_evento_ufsm_evento
                                 FOREIGN KEY (id_evento) REFERENCES evento(id_evento),

                             CONSTRAINT fk_evento_ufsm_inversor
                                 FOREIGN KEY (id_inversor) REFERENCES inversor(id_inversor)
);

CREATE TABLE evento_energy2go (
                                  id SERIAL PRIMARY KEY,

                                  id_evento INTEGER NOT NULL,

                                  id_inversor INTEGER NOT NULL,

                                  data_inicial TIMESTAMP NOT NULL,
                                  data_final TIMESTAMP NOT NULL,
                                  potencia DOUBLE PRECISION,

                                  CONSTRAINT fk_evento_energy_evento
                                      FOREIGN KEY (id_evento) REFERENCES evento(id_evento),

                                  CONSTRAINT fk_evento_energy_inversor
                                      FOREIGN KEY (id_inversor) REFERENCES inversor(id_inversor)
);