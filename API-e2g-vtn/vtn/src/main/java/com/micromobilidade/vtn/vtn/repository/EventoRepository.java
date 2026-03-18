package com.micromobilidade.vtn.vtn.repository;

import com.micromobilidade.vtn.vtn.entity.EventoEntity;
import com.micromobilidade.vtn.vtn.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<EventoEntity, Integer> {


    List<EventoEntity> findByDataInicialAfter(LocalDateTime agora);

    Optional<EventoEntity> findByChave(String chave);
}
