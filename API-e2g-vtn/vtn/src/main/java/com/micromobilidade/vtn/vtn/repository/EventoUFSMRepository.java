package com.micromobilidade.vtn.vtn.repository;

import com.micromobilidade.vtn.vtn.entity.EventoEntity;
import com.micromobilidade.vtn.vtn.entity.EventoUFSMEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventoUFSMRepository extends JpaRepository<EventoUFSMEntity, Long> {

    Optional<EventoUFSMEntity> findByIdApi(String idApi);

    EventoUFSMEntity findByEvento(EventoEntity evento);


}
