package com.micromobilidade.vtn.vtn.model;

import java.time.LocalDateTime;
import java.util.List;

public record RespostaBackToFrontDTO (Integer id,
                                      double potencialTotal,
                                      TipoEvento type,
                                      LocalDateTime dataInicial,
                                      LocalDateTime dataFinal,
                                      List<InversorPotenciaDTO>potenciasInversores){
}
