package com.micromobilidade.vtn.vtn.model;

public record EventoFrontDTO(String id,
                             double value,
                             TipoEvento type,
                             Long startTime,
                             Long endTime) {
}
