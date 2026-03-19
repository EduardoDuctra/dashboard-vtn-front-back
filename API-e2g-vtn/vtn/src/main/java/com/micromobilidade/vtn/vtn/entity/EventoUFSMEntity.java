package com.micromobilidade.vtn.vtn.entity;

import com.micromobilidade.vtn.vtn.model.StatusEvento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_ufsm")
@Getter @Setter
public class EventoUFSMEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_evento", nullable = false)
    private EventoEntity evento;

    @ManyToOne
    @JoinColumn(name = "id_inversor", nullable = false)
    private InversorEntity inversor;

    @Column(name = "id_api")
    private String idApi;


    @Column(name = "data_inicial")
    private LocalDateTime dataInicial;

    @Column(name = "data_final")
    private LocalDateTime dataFinal;

    @Column(name = "potencia")
    private Double potencia;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StatusEvento status;
}
