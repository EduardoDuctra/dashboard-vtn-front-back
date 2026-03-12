package com.micromobilidade.vtn.vtn.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inversor")
@Getter
@Setter
public class InversorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inversor")
    private Integer id;

    @Column( name = "descricao")
    private String descricao;

    @Column(name = "local")
    private String local;

    @Column(name = "potencia_maxima_w")
    private Double potenciaMaximaW;


    @Column(name = "capacidade_maxima_bateria_w")
    private Double capacidadeMaximaBateriaW;

    @Column(name = "quantidade_baterias")
    private Integer quantidadeBaterias;

    @Column(name = "potencia_maxima_descarga_por_bateria_w")
    private Double potenciaMaximaDescargaPorBateriaW;


}
