package com.micromobilidade.vtn.vtn.service;

import com.micromobilidade.vtn.vtn.entity.EventoEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEventoEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEventoId;
import com.micromobilidade.vtn.vtn.model.EventoAgendadoDTO;
import com.micromobilidade.vtn.vtn.model.EventoFrontDTO;
import com.micromobilidade.vtn.vtn.model.EventoImediatoDTO;
import com.micromobilidade.vtn.vtn.model.TipoEventoUFSM;
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
    private final String url;

    private final ApiRepository apiRepository;
    private final InversorEventoRepository inversorEventoRepository;
    private final InversorRepository inversorRepository;


    int idInversorUfsm = 1;
    int idInversorE2G = 2;


    public EventoService(
            @Value("${username}") String username,
            @Value("${password}") String password,
            @Value("${url}") String url, ApiRepository apiRepositori, InversorEventoRepository inversorEventoRepository, InversorRepository inversorRepository
    ) {


        this.url = url;
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
        importarEventoAPI();
    }

    public void cadastrarEventoBanco (EventoFrontDTO eventoFrontDTO) {

        String resposta = publicarDTOApiJeann(eventoFrontDTO);

        if(resposta == null){
            throw new RuntimeException("Falha ao publicar evento na API");
        }

        salvarEventoBanco(eventoFrontDTO);
    }

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



        Double valor = calculoInversor(idInversorUfsm, eventoFrontDTO.value(), eventoFrontDTO.type());

        // UFSM trabalha com %
        if (eventoFrontDTO.type() == TipoEventoUFSM.inject) {
            valor = calculoPotenciaDisponivel(
                    eventoFrontDTO.value(),
                    idInversorUfsm
            );
        }

        if (eventoFrontDTO.startTime() <= agora) {


            long duracaoMs = eventoFrontDTO.endTime() - agora;
            int duracaoMin = (int) Math.ceil(duracaoMs / 60000.0);

            EventoImediatoDTO eventoImediatoDTO =
                    new EventoImediatoDTO(valor, duracaoMin);

            try {

                System.out.println(url + "/cmd/" + eventoFrontDTO.type());

                return restClient.post()
                        .uri(url + "/cmd/" + eventoFrontDTO.type())
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

            EventoAgendadoDTO eventoAgendadoDTO = new EventoAgendadoDTO(valor, startIso,  endIso);


            try {

                System.out.println("Endpoint schedule: " + url + "/schedule/" + eventoFrontDTO.type());
                return restClient.post()
                        .uri(url + "/schedule/" + eventoFrontDTO.type())
                        .body(eventoAgendadoDTO)
                        .retrieve()
                        .body(String.class);

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }


    public EventoFrontDTO[] buscarEventos(){

        try {
            return restClient.get()
                    .uri(url+"/events")
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

    public void importarEventoAPI(){

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
                    .uri(url + "/" + id)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean verificarEvento(long dataInicial, long dataFinal){
        EventoFrontDTO[] eventos = restClient.get()
                .uri(url+"/events")
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
    public Double calculoPotenciaDisponivel(Double potencia, Integer idInversor) {


        InversorEntity inversor = inversorRepository.findById(idInversor)
                .orElseThrow(() -> new RuntimeException("Inversor não encontrado"));


        Double valorMaximoDescarga =
                inversor.getPotenciaMaximaDescargaPorBateriaW() * inversor.getQuantidadeBaterias();

        Double porcentagemPotencia =
                (potencia * 100) / valorMaximoDescarga;

        if (porcentagemPotencia > 100) {
            porcentagemPotencia = 100.0;
        }

        return porcentagemPotencia;
    }
}
