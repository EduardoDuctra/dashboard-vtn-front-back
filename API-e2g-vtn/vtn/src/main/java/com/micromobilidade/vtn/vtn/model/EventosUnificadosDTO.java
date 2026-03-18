package com.micromobilidade.vtn.vtn.model;

import com.micromobilidade.vtn.vtn.entity.EventoEntity;

public record EventosUnificadosDTO(Integer id,
                                   double value,
                                   TipoEvento type,
                                   Long startTime,
                                   Long endTime,
                                   String apiId) {
}
