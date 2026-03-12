package com.micromobilidade.vtn.vtn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InversorEventoId implements Serializable {

    @Column(name = "id_evento")
    private Integer idEvento;

    @Column(name = "id_inversor")
    private Integer idInversor;
}
