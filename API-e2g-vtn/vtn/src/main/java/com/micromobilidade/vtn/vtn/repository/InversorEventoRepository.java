package com.micromobilidade.vtn.vtn.repository;

import com.micromobilidade.vtn.vtn.entity.InversorEventoEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEventoId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InversorEventoRepository extends JpaRepository<InversorEventoEntity, InversorEventoId> {
}
