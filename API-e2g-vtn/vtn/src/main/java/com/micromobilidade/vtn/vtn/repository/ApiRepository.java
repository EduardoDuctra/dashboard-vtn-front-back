package com.micromobilidade.vtn.vtn.repository;

import com.micromobilidade.vtn.vtn.entity.EventoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ApiRepository extends JpaRepository<EventoEntity, Integer> {

    boolean existsByDataInicial(LocalDateTime dataInicial);

    List<EventoEntity> findByDataInicialAfter(LocalDateTime agora);
}
