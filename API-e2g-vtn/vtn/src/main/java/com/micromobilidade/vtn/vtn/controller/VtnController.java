package com.micromobilidade.vtn.vtn.controller;

import com.micromobilidade.vtn.vtn.model.VtnDTO;
import com.micromobilidade.vtn.vtn.service.VtnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vtn")
public class VtnController {

    private final VtnService service;

    public VtnController(VtnService service) {
        this.service = service;
    }


    @PostMapping("/create")
    public ResponseEntity<?> criarEvento(@RequestBody VtnDTO dto) {

        long dataInicial = dto.startTime();
        long dataFinal = dto.endTime();

        boolean conflito = service.verificarEvento(dataInicial, dataFinal);

        if(conflito){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Evento já existente nesse intervalo de tempo");
        }
      else {
            return ResponseEntity.ok(service.publicarDTO(dto));
        }
    }

    @GetMapping("/events")
    public ResponseEntity<?> listarEventos() {
        return ResponseEntity.ok(service.buscarEventos());

    }


//    d
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarEvento(@PathVariable String id){
        return ResponseEntity.ok(service.deletarEvento(id));
    }
}
