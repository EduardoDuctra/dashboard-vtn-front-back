package com.micromobilidade.vtn.vtn.entity;

import com.micromobilidade.vtn.vtn.model.TipoEventoUFSM;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento")
@Getter @Setter
public class EventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer id;

    @Column(name = "data_inicial")
    private LocalDateTime dataInicial;

    @Column(name = "data_final")
    private LocalDateTime dataFinal;


    @Column(name = "potencia_solicitada_kw")
    private Double potenciaSolicitadaKw;

    @Column(name = "tipo_evento")
    @Enumerated(EnumType.STRING)
    private TipoEventoUFSM tipoEventoUFSM;


}

