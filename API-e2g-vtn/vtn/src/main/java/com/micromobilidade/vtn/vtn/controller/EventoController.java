package com.micromobilidade.vtn.vtn.controller;

import com.micromobilidade.vtn.vtn.model.EventoDTOUFSM;
import com.micromobilidade.vtn.vtn.service.EventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vtn")
public class EventoController {

    private final EventoService service;


    public EventoController(EventoService service) {
        this.service = service;
    }


    @PostMapping("/create")
    public ResponseEntity<?> criarEvento(@RequestBody EventoDTOUFSM dto) {

        long dataInicial = dto.startTime();
        long dataFinal = dto.endTime();

        boolean conflito = service.verificarEvento(dataInicial, dataFinal);

        if(conflito){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Evento já existente nesse intervalo de tempo");
        }
      else {
            System.out.println("Valor:" + dto.value());
            System.out.println("Data inicial:" + dataInicial);
            System.out.println("Data fim:" + dataFinal);


            service.cadastrarEventoBanco(dto);
            return ResponseEntity.ok("Evento criado com sucesso");
        }
    }

    @GetMapping("/events")
    public ResponseEntity<?> listarEventos() {
        return ResponseEntity.ok(service.buscarEventos());

    }



    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarEvento(@PathVariable String id){
        return ResponseEntity.ok(service.deletarEvento(id));
    }
}
