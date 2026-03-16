package com.micromobilidade.vtn.vtn.model;

import java.time.LocalDateTime;
import java.util.List;

public record RespostaBackToFrontDTO (Integer id,
                                      String idApiUfsm,
                                      double potencialTotal,
                                      TipoEvento type,
                                      LocalDateTime dataInicial,
                                      LocalDateTime dataFinal,
                                      List<InversoresDTO> inversores){
}
