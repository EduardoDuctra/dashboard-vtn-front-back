package com.micromobilidade.vtn.vtn.service;

import com.micromobilidade.vtn.vtn.entity.EventoEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEventoEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEventoId;
import com.micromobilidade.vtn.vtn.model.*;
import com.micromobilidade.vtn.vtn.repository.ApiRepository;
import com.micromobilidade.vtn.vtn.repository.InversorEventoRepository;
import com.micromobilidade.vtn.vtn.repository.InversorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class EventoService {

    private final RestClient restClient;
    private final String urlUFSM;
    private final String urlEnergy2Go;

    private final ApiRepository apiRepository;
    private final InversorEventoRepository inversorEventoRepository;
    private final InversorRepository inversorRepository;


    int idInversorUfsm = 1;
    int idInversorE2G = 2;


    public EventoService(
            @Value("${username}") String username,
            @Value("${password}") String password,
            @Value("${url}") String urlUFSM,
            @Value("${urlEnergy2Go}") String urlEnergy2Go,
            ApiRepository apiRepositori,
            InversorEventoRepository inversorEventoRepository,
            InversorRepository inversorRepository
    ) {


        this.urlUFSM = urlUFSM;
        this.urlEnergy2Go = urlEnergy2Go;
        this.apiRepository = apiRepositori;
        this.inversorEventoRepository = inversorEventoRepository;
        this.inversorRepository = inversorRepository;

        this.restClient = RestClient.builder()
                .defaultHeaders(headers ->
                        headers.setBasicAuth(username, password)
                )
                .build();
    }


    @PostConstruct
    public void carregarEventosAPI() {
        importarEventoApiUfsm();
    }


    public void cadastrarEventoBanco (EventoFrontDTO eventoFrontDTO) {

        String respostaUFSM = publicarDTOApiJeann(eventoFrontDTO);
        System.out.println("Resposta UFSM: " + respostaUFSM);

        String respostaEnergy = publicarDTOApiEnergy2Go(eventoFrontDTO);
        System.out.println("Resposta Energy2Go: " + respostaEnergy);


        salvarEventoBanco(eventoFrontDTO);
    }

    //dividir potencia pela quantidade de inversores
    public Double calculoInversor(Integer idInversor, Double valor, TipoEventoUFSM tipoEventoUFSM) {

        InversorEntity inversor = inversorRepository.findById(idInversor)
                .orElseThrow(() -> new RuntimeException("Inversor não encontrado"));


        Double valorMaximoDescarga =
                inversor.getPotenciaMaximaDescargaPorBateriaW() * inversor.getQuantidadeBaterias();


        Double valorCalculado = valor / inversorRepository.count();

        if (valorCalculado > inversor.getPotenciaMaximaW() && tipoEventoUFSM == TipoEventoUFSM.limit_charge) {
            valorCalculado = inversor.getPotenciaMaximaW();
        }
        else if (valorCalculado > valorMaximoDescarga && tipoEventoUFSM == TipoEventoUFSM.inject) {
            valorCalculado = valorMaximoDescarga;
        }

        return valorCalculado;
    }


    public String publicarDTOApiJeann(EventoFrontDTO eventoFrontDTO) {

        long agora = System.currentTimeMillis();



        // inversor trabalha com %
        Double valorPorcentagem = calculoPotenciaDisponivel(eventoFrontDTO, idInversorUfsm);

        if (eventoFrontDTO.startTime() <= agora) {


            long duracaoMs = eventoFrontDTO.endTime() - agora;
            int duracaoMin = (int) Math.ceil(duracaoMs / 60000.0);

            EventoImediatoDTO eventoImediatoDTO =
                    new EventoImediatoDTO(valorPorcentagem, duracaoMin);

            try {

                System.out.println(urlUFSM + "/cmd/" + eventoFrontDTO.type());

                return restClient.post()
                        .uri(urlUFSM + "/cmd/" + eventoFrontDTO.type())
                        .body(eventoImediatoDTO)
                        .retrieve()
                        .body(String.class);

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }


        else {

            String startIso = Instant.ofEpochMilli(eventoFrontDTO.startTime()).toString();
            String endIso = Instant.ofEpochMilli(eventoFrontDTO.endTime()).toString();

            EventoAgendadoDTO eventoAgendadoDTO = new EventoAgendadoDTO(valorPorcentagem, startIso,  endIso);


            try {

                System.out.println("Endpoint schedule: " + urlUFSM + "/schedule/" + eventoFrontDTO.type());
                return restClient.post()
                        .uri(urlUFSM + "/schedule/" + eventoFrontDTO.type())
                        .body(eventoAgendadoDTO)
                        .retrieve()
                        .body(String.class);

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }


    public String publicarDTOApiEnergy2Go(EventoFrontDTO eventoFrontDTO) {

        // inversor trabalha com %
        Double valorPorcentagem = calculoPotenciaDisponivel(eventoFrontDTO, idInversorE2G);

        EventoDTOE2G dtoEnergy = new EventoDTOE2G(valorPorcentagem);


        try {

            System.out.println(urlEnergy2Go + "/" + eventoFrontDTO.type());

            return restClient.post()
                    .uri(urlEnergy2Go + "/" + eventoFrontDTO.type())
                    .body(dtoEnergy)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    public EventoFrontDTO[] buscarEventos(){

        try {
            return restClient.get()
                    .uri(urlUFSM +"/events")
                    .retrieve()
                    .body(EventoFrontDTO[].class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void salvarEventoBanco(EventoFrontDTO dto) {

        LocalDateTime dataInicial = Instant.ofEpochMilli(dto.startTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .withNano(0);

        LocalDateTime dataFinal = Instant.ofEpochMilli(dto.endTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .withNano(0);



        EventoEntity evento = new EventoEntity();
        evento.setDataInicial(dataInicial);
        evento.setDataFinal(dataFinal);
        evento.setPotenciaSolicitadaKw(dto.value());
        evento.setTipoEventoUFSM(dto.type());


        EventoEntity eventoSalvo = this.apiRepository.save(evento);


        List<InversorEntity> inversores = inversorRepository.findAll();


        for(InversorEntity inversor : inversores) {


            Double valor = calculoInversor(inversor.getId(), dto.value(), dto.type());

            InversorEventoId inversorEventoId = new InversorEventoId();
            inversorEventoId.setIdEvento(eventoSalvo.getId());
            inversorEventoId.setIdInversor(inversor.getId());



            InversorEventoEntity inversorEvento = new InversorEventoEntity();
            inversorEvento.setId(inversorEventoId);
            inversorEvento.setPotenciaEntregueKw(valor);

            this.inversorEventoRepository.save(inversorEvento);

        }




    }

    public void importarEventoApiUfsm(){

        EventoFrontDTO[] apiDTO = buscarEventos();

        for(EventoFrontDTO dto : apiDTO){

            LocalDateTime dataInicial = Instant.ofEpochMilli(dto.startTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .withNano(0);

            if(apiRepository.existsByDataInicial(dataInicial)){
                continue;
            }

            salvarEventoBanco(dto);
        }
    }
    public String deletarEvento(String id){

        try {
            return restClient.delete()
                    .uri(urlUFSM + "/" + id)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean verificarEvento(long dataInicial, long dataFinal){
        EventoFrontDTO[] eventos = restClient.get()
                .uri(urlUFSM +"/events")
                .retrieve()
                .body(EventoFrontDTO[].class);

        for(EventoFrontDTO evento : eventos){
            long dataInicialDTO = evento.startTime();
            long dataFinalDTO = evento.endTime();

            boolean conflito = (dataInicial < dataFinalDTO) && (dataInicialDTO < dataFinal);

            if (conflito){
                return true;
            }
        }

        return false;
    }


    //preciso pq o inversor da UFSM é por % e no input eu mando o valor em watts
    public Double calculoPotenciaDisponivel(EventoFrontDTO dto, Integer idInversor) {


        InversorEntity inversor = inversorRepository.findById(idInversor)
                .orElseThrow(() -> new RuntimeException("Inversor não encontrado"));

        Double potenciaInversor = calculoInversor(inversor.getId(), dto.value(), dto.type());

        Double porcentagemPotencia =0.0;

        if(dto.type()==TipoEventoUFSM.inject){

            Double valorMaximoDescarga =
                    inversor.getPotenciaMaximaDescargaPorBateriaW() * inversor.getQuantidadeBaterias();

            porcentagemPotencia =
                    (potenciaInversor * 100) / valorMaximoDescarga;


        } else{
            porcentagemPotencia =
                    (potenciaInversor * 100) / inversor.getPotenciaMaximaW();

        }

        if (porcentagemPotencia > 100) {
            porcentagemPotencia = 100.0;
        }

        return porcentagemPotencia;
    }
}
