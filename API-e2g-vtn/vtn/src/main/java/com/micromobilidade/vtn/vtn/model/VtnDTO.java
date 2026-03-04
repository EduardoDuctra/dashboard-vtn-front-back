package com.micromobilidade.vtn.vtn.model;

import java.time.LocalDateTime;

public record VtnDTO(String id,
                     double value,
                     String type,
                     Long startTime,
                     Long endTime) {
}
