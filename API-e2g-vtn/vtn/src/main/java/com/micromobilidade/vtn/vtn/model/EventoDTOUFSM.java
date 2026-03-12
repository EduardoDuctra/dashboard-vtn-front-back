package com.micromobilidade.vtn.vtn.model;

public record EventoDTOUFSM(String id,
                            double value,
                            TipoEventoUFSM type,
                            Long startTime,
                            Long endTime) {
}
