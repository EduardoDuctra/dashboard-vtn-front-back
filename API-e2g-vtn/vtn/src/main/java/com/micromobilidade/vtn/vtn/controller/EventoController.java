package com.micromobilidade.vtn.vtn.controller;

import com.micromobilidade.vtn.vtn.model.EventoFrontDTO;
import com.micromobilidade.vtn.vtn.service.EventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/vtn")
public class EventoController {

    private final EventoService service;


    public EventoController(EventoService service) {
        this.service = service;
    }


    @PostMapping("/create")
    public ResponseEntity<?> criarEvento(@RequestBody EventoFrontDTO dto) {

        long dataInicial = dto.startTime();
        long dataFinal = dto.endTime();

        boolean conflito = service.verificarEvento(dataInicial, dataFinal);

        //agora + tolerancia 20s
        long agora = System.currentTimeMillis() - 10000;


        if(dto.startTime() < agora){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Data invalida!"));
        }

        if(conflito){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message","Evento já existente nesse intervalo de tempo"));
        }
      else {
            System.out.println("Valor:" + dto.value());
            System.out.println("Data inicial:" + dataInicial);
            System.out.println("Data fim:" + dataFinal);


            service.cadastrarEventoBanco(dto);
            return ResponseEntity.ok().build();
        }
    }

    @GetMapping("/events")
    public ResponseEntity<?> listarEventos() {
        return ResponseEntity.ok(service.buscarEventos());

    }


    @GetMapping("/events-complete")
    public ResponseEntity<?> buscarEventosBanco() {
        return ResponseEntity.ok(service.buscarEventosBanco());
    }





    @DeleteMapping("/events/{apiId}")
    public ResponseEntity<?> deletarEvento(@PathVariable String apiId){


        service.deletarEvento(apiId);
        return ResponseEntity.ok().build();
    }
}
