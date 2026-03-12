package com.micromobilidade.vtn.vtn.model;

public record EventoFrontDTO(String id,
                             double value,
                             TipoEventoUFSM type,
                             Long startTime,
                             Long endTime) {
}
