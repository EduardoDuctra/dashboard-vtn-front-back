package com.micromobilidade.vtn.vtn.model;


public record EventosUnificadosDTO(Integer id,
                                   double value,
                                   TipoEvento type,
                                   Long startTime,
                                   Long endTime,
                                   String apiId,
                                   String status) {
}
