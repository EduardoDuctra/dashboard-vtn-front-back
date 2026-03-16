package com.micromobilidade.vtn.vtn.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inversor_evento")
@Getter @Setter
public class InversorEventoEntity {


    @EmbeddedId
    private InversorEventoId id;

    @Column(name = "potencia_entregue_kw")
    private Double potenciaEntregueKw;

    @ManyToOne
    @JoinColumn(name = "id_inversor", insertable = false, updatable = false)
    private InversorEntity inversor;

    @Column(name = "id_evento_api_ufsm")
    private String idApiUFSM;

}
