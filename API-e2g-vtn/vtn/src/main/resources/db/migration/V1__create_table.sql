create table inversor(
    id_inversor serial primary key,
    descricao varchar(50),
    local varchar(50),
    potencia_maxima_w double precision,
    capacidade_maxima_bateria_w double precision,
    quantidade_baterias integer,
    potencia_maxima_descarga_por_bateria_w double precision
);

CREATE TABLE evento (
    id_evento SERIAL PRIMARY KEY,
    data_inicial TIMESTAMP NOT NULL,
    data_final TIMESTAMP NOT NULL,
    potencia_solicitada_kw DOUBLE PRECISION NOT NULL
);
CREATE TABLE inversor_evento (
    id_evento INTEGER,
    id_inversor INTEGER,
    potencia_entregue_kw DOUBLE PRECISION,

    PRIMARY KEY (id_evento, id_inversor),

    FOREIGN KEY (id_evento) REFERENCES evento(id_evento),
    FOREIGN KEY (id_inversor) REFERENCES inversor(id_inversor)
);

