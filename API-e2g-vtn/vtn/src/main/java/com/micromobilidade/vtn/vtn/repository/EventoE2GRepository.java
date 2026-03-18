package com.micromobilidade.vtn.vtn.repository;

import com.micromobilidade.vtn.vtn.entity.EventoEnergy2GoEntity;
import com.micromobilidade.vtn.vtn.entity.EventoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoE2GRepository extends JpaRepository<EventoEnergy2GoEntity, Integer> {

    EventoEnergy2GoEntity findByEvento(EventoEntity evento);

}
