package com.micromobilidade.vtn.vtn.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

}
