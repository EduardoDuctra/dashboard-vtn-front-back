package com.micromobilidade.vtn.vtn.service;

import com.micromobilidade.vtn.vtn.entity.EventoEnergy2GoEntity;
import com.micromobilidade.vtn.vtn.entity.EventoEntity;
import com.micromobilidade.vtn.vtn.entity.EventoUFSMEntity;
import com.micromobilidade.vtn.vtn.entity.InversorEntity;
import com.micromobilidade.vtn.vtn.model.*;
import com.micromobilidade.vtn.vtn.repository.EventoE2GRepository;
import com.micromobilidade.vtn.vtn.repository.EventoRepository;
import com.micromobilidade.vtn.vtn.repository.EventoUFSMRepository;
import com.micromobilidade.vtn.vtn.repository.InversorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EventoService {

    private final RestClient restClient;
    private final String urlUFSM;
    private final String urlEnergy2Go;

    private final EventoRepository eventoRepository;
    private final InversorRepository inversorRepository;
    private final EventoUFSMRepository eventoUFSMRepository;
    private final EventoE2GRepository eventoE2GRepository;


    int idInversorUfsm = 1;
    int idInversorE2G = 2;


    public EventoService(
            @Value("${username}") String username,
            @Value("${password}") String password,
            @Value("${url}") String urlUFSM,
            @Value("${urlEnergy2Go}") String urlEnergy2Go,
            EventoRepository eventoRepository,
            InversorRepository inversorRepository, EventoUFSMRepository eventoUFSMRepository, EventoE2GRepository eventoE2GRepository
    ) {


        this.urlUFSM = urlUFSM;
        this.urlEnergy2Go = urlEnergy2Go;
        this.eventoRepository = eventoRepository;
        this.inversorRepository = inversorRepository;
        this.eventoUFSMRepository = eventoUFSMRepository;
        this.eventoE2GRepository = eventoE2GRepository;

        this.restClient = RestClient.builder()
                .defaultHeaders(headers ->
                        headers.setBasicAuth(username, password)
                )
                .build();
    }


    @PostConstruct
    public void carregarEventosAPI() {
        importarEventoApiUfsm();

        List<EventoEntity> eventos = eventoRepository.findByDataInicialAfter(LocalDateTime.now());


        //preciso buscar do banco e converter num DTO e mando para -> agendarEventoEnergy
        for(EventoEntity evento : eventos) {

            EventoFrontDTO dto = new EventoFrontDTO(
                    evento.getId().toString(),
                    evento.getPotenciaSolicitadaKw(),
                    evento.getTipoEventoUFSM(),
                    evento.getDataInicial()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli(),
                    evento.getDataFinal()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );

            agendarEventoEnergy(dto);
        }
    }


    public void cadastrarEventoBanco (EventoFrontDTO eventoFrontDTO) {

        System.out.println("Valor recebido do front: "+eventoFrontDTO.value());

        String respostaUFSM = publicarDTOApiUFSM(eventoFrontDTO);
        System.out.println("Resposta UFSM: " + respostaUFSM);

        String respostaEnergy = agendarEventoEnergy(eventoFrontDTO);
        System.out.println("Resposta Energy2Go: " + respostaEnergy);
        EventoEntity evento = null;

        try {
            //salvar em evento
            evento = salvarEventoBanco(eventoFrontDTO);
        } catch (Exception e) {
            System.out.println("Erro ao salvar do Evento");
            throw new RuntimeException(e);
        }

        try {
            //salvar nas respectivas tabelas UFSM e E2G
            //busco na API -> acho o ID
            EventoFrontDTO retornoComID = buscarEventoApi(eventoFrontDTO);
            salvarEventoApiBanco(evento, retornoComID);

            salvarEventoEnergy2Go(evento, eventoFrontDTO);
        } catch (Exception e) {
            System.out.println("Erro ao salvar do Evento nas tabelas UFSM ou Energy2GO");
            throw new RuntimeException(e);
        }


    }



    @Transactional
    public EventoEntity salvarEventoBanco(EventoFrontDTO dto) {

        LocalDateTime dataInicial = Instant.ofEpochMilli(dto.startTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        LocalDateTime dataFinal = Instant.ofEpochMilli(dto.endTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        EventoEntity novoEvento = new EventoEntity();

        String chave = dto.type() + "_" + dataInicial + "_" + dataFinal;

        Optional<EventoEntity> existente = eventoRepository.findByChave(chave);

        if (existente.isPresent()) {
            return existente.get();
        }

        novoEvento.setDataInicial(dataInicial);
        novoEvento.setDataFinal(dataFinal);
        novoEvento.setPotenciaSolicitadaKw(dto.value());
        novoEvento.setTipoEventoUFSM(dto.type());
        novoEvento.setChave(chave);
        novoEvento.setStatus(StatusEvento.ATIVO);

        try {
            return eventoRepository.save(novoEvento);
        } catch (Exception e) {
            System.out.println("Erro ao salvar do Evento no banco");
            return eventoRepository.findByChave(chave).orElseThrow();
        }
    }



    @Transactional
    public void salvarEventoApiBanco(EventoEntity evento, EventoFrontDTO dto) {

        LocalDateTime dataInicial = Instant.ofEpochMilli(dto.startTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        LocalDateTime dataFinal = Instant.ofEpochMilli(dto.endTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);


        InversorEntity inversor = inversorRepository.findById(idInversorUfsm)
                .orElseThrow(() -> new RuntimeException("Evento interno não encontrado"));

        double valorPotenciaW = calcularPorcentagemPotencia(dto.value(), inversor.getId(), dto.type());

        EventoUFSMEntity eventoUFSMEntity = new EventoUFSMEntity();
        eventoUFSMEntity.setEvento(evento);
        eventoUFSMEntity.setDataInicial(dataInicial);
        eventoUFSMEntity.setDataFinal(dataFinal);
        eventoUFSMEntity.setIdApi(dto.id());
        eventoUFSMEntity.setInversor(inversor);
        eventoUFSMEntity.setPotencia(valorPotenciaW);
        eventoUFSMEntity.setStatus(StatusEvento.ATIVO);


        try {
            eventoUFSMRepository.save(eventoUFSMEntity);
        } catch (Exception e) {
            System.out.println("Erro ao salvar na tabela EventoUFSM");
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void salvarEventoEnergy2Go(EventoEntity evento, EventoFrontDTO eventoFrontDTO) {

        LocalDateTime dataInicial = Instant.ofEpochMilli(eventoFrontDTO.startTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        LocalDateTime dataFinal = Instant.ofEpochMilli(eventoFrontDTO.endTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        InversorEntity inversor = inversorRepository.findById(idInversorE2G)
                .orElseThrow(() -> new RuntimeException("Evento interno não encontrado"));


        double valorPotenciaW = calculoInversor(inversor.getId(), eventoFrontDTO.value(), eventoFrontDTO.type());

        EventoEnergy2GoEntity eventoE2G = new EventoEnergy2GoEntity();
        eventoE2G.setEvento(evento);
        eventoE2G.setDataInicial(dataInicial);
        eventoE2G.setDataFinal(dataFinal);
        eventoE2G.setPotencia(valorPotenciaW);
        eventoE2G.setInversor(inversor);
        eventoE2G.setStatus(StatusEvento.ATIVO);

        try {
            eventoE2GRepository.save(eventoE2G);
        } catch (Exception e) {
            System.out.println("Erro ao salvar na tabela EventoEnergy2Go");
            throw new RuntimeException(e);
        }


    }


    //nao mexer
    //dividir potencia pela quantidade de inversores
    public Double calculoInversor(Integer idInversor, Double valor, TipoEvento tipoEventoUFSM) {

        InversorEntity inversor = inversorRepository.findById(idInversor)
                .orElseThrow(() -> new RuntimeException("Inversor não encontrado"));


        Double valorMaximoDescarga =
                inversor.getPotenciaMaximaDescargaPorBateriaW() * inversor.getQuantidadeBaterias();


        Double valorCalculado = valor / inversorRepository.count();

        if (valorCalculado > inversor.getPotenciaMaximaW() && tipoEventoUFSM == tipoEventoUFSM.limit_charge) {
            valorCalculado = inversor.getPotenciaMaximaW();
        }
        else if (valorCalculado > valorMaximoDescarga && tipoEventoUFSM == tipoEventoUFSM.inject) {
            valorCalculado = valorMaximoDescarga;
        }

        return valorCalculado;
    }



    public String publicarDTOApiUFSM(EventoFrontDTO eventoFrontDTO) {

        long agora = System.currentTimeMillis();



        // inversor trabalha com %
        Double valorPorcentagem = calculoPotenciaParaPorcentagem(eventoFrontDTO, idInversorUfsm);

        if (eventoFrontDTO.startTime() <= agora) {


            long duracaoMs = eventoFrontDTO.endTime() - agora;
            long duracaoSegundos = (long) Math.ceil(duracaoMs / 1000.0);

            EventoImediatoDTO eventoImediatoDTO =
                    new EventoImediatoDTO(valorPorcentagem, (int) duracaoSegundos);

            try {

                System.out.println(urlUFSM + "/cmd/" + eventoFrontDTO.type());

                String resposta = restClient.post()
                        .uri(urlUFSM + "/cmd/" + eventoFrontDTO.type())
                        .body(eventoImediatoDTO)
                        .retrieve()
                        .body(String.class);


                return resposta;

            } catch (Exception e) {
                System.out.println("Erro ao publicar API UFSM");
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
                String resposta = restClient.post()
                        .uri(urlUFSM + "/schedule/" + eventoFrontDTO.type())
                        .body(eventoAgendadoDTO)
                        .retrieve()
                        .body(String.class);


                return resposta;

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    //ok nao mexo
    public String publicarDTOApiEnergy2Go(EventoFrontDTO eventoFrontDTO) {

        // inversor trabalha com %
        Double valorPorcentagem = calculoPotenciaParaPorcentagem(eventoFrontDTO, idInversorE2G);

        EventoDTOE2G dtoEnergy = new EventoDTOE2G(valorPorcentagem);


        try {

            System.out.println(urlEnergy2Go + "/" + eventoFrontDTO.type());

            return restClient.post()
                    .uri(urlEnergy2Go + "/" + eventoFrontDTO.type())
                    .body(dtoEnergy)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            System.out.println("Erro ao publicar API ENERGY");
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


    //busco na API para pegar o ID com base na data e tipo evento (tabela chave)
    public EventoFrontDTO buscarEventoApi(EventoFrontDTO dto) {

        Double valorConvertido = calculoPotenciaParaPorcentagem(dto, idInversorUfsm);


        LocalDateTime dataInicialDto = Instant.ofEpochMilli(dto.startTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        LocalDateTime dataFinalDto = Instant.ofEpochMilli(dto.endTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        String chaveDto = dto.type() + "_" + dataInicialDto + "_" + dataFinalDto;

        EventoFrontDTO[] eventos = buscarEventos();

        for (EventoFrontDTO evento : eventos) {


            LocalDateTime dataInicialApi = Instant.ofEpochMilli(evento.startTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .truncatedTo(ChronoUnit.MINUTES);

            LocalDateTime dataFinalApi = Instant.ofEpochMilli(evento.endTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .truncatedTo(ChronoUnit.MINUTES);

            String chaveApi = evento.type() + "_" + dataInicialApi + "_" + dataFinalApi;

            boolean mesmaChave = chaveApi.equals(chaveDto);
            boolean mesmaPotencia = Math.abs(evento.value() - valorConvertido) < 1;

            if (mesmaChave && mesmaPotencia) {
                System.out.println("Evento encontrado: " + evento.id());
                return evento;
            }
        }

        throw new RuntimeException("Evento não apareceu na API");
    }

    //nao mexer pq ta funcionado
    public void importarEventoApiUfsm(){

        EventoFrontDTO[] apiDTO = buscarEventos();

        for(EventoFrontDTO dto : apiDTO){

            LocalDateTime dataInicial = Instant.ofEpochMilli(dto.startTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .truncatedTo(ChronoUnit.MINUTES);

            LocalDateTime dataFinal = Instant.ofEpochMilli(dto.endTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .truncatedTo(ChronoUnit.MINUTES);

            double valor = calcularPorcentagemPotencia(dto.value(), idInversorUfsm, dto.type());

            EventoUFSMEntity eventoUfsm = eventoUFSMRepository
                    .findByIdApi(dto.id())
                    .orElse(new EventoUFSMEntity());

            eventoUfsm.setIdApi(dto.id());
            eventoUfsm.setDataInicial(dataInicial);
            eventoUfsm.setDataFinal(dataFinal);
            eventoUfsm.setPotencia(valor);
            eventoUfsm.setStatus(StatusEvento.ATIVO);


            InversorEntity inversor = inversorRepository.findById(idInversorUfsm).orElse(null);

            eventoUfsm.setInversor(inversor);


            String chave = dto.type() + "_" + dataInicial + "_" + dataFinal;

            Optional<EventoEntity> eventoOpt = eventoRepository.findByChave(chave);

            EventoEntity evento = eventoOpt.orElseGet(() -> {
                EventoEntity novo = new EventoEntity();
                novo.setDataInicial(dataInicial);
                novo.setDataFinal(dataFinal);
                novo.setChave(chave);
                novo.setPotenciaSolicitadaKw(valor);
                novo.setTipoEventoUFSM(dto.type());
                novo.setStatus(StatusEvento.ATIVO);
                return eventoRepository.save(novo);
            });

            eventoUfsm.setEvento(evento);

            try {
                eventoUFSMRepository.save(eventoUfsm);
            } catch (Exception e) {
                System.out.println("Erro ao salvar: " + e.getMessage());
                throw new RuntimeException(e);
            }


        }
    }

    public String deletarEventoAPIJean(String apiId){

        try {
            return restClient.delete()
                    .uri(urlUFSM + "/events/" + apiId)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            System.out.println("Erro ao deletar: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }



    public String deletarEventoEnergy2Go() {




        EventoDTOE2G dtoEnergy = new EventoDTOE2G(0.0);


        try {

            System.out.println(urlEnergy2Go + "/" + TipoEvento.inject);

            return restClient.post()
                    .uri(urlEnergy2Go + "/" + TipoEvento.inject)
                    .body(dtoEnergy)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            System.out.println("Erro ao deletar: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

    }


    public void deletarEvento(String apiId) {


        System.out.println("ID APi no deletar: " + apiId);

        EventoUFSMEntity ufsm = eventoUFSMRepository.findByIdApi(apiId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        EventoEntity evento = ufsm.getEvento();

        System.out.println("ID do evento: " + evento.getId());


        try {
            deletarEventoAPIJean(apiId);
            ufsm.setStatus(StatusEvento.INATIVO);

            eventoUFSMRepository.save(ufsm);

        } catch (Exception e) {
            System.out.println("Erro ao deletar no repositório da UFSM: " + e.getMessage());
            throw new RuntimeException(e);
        }


        EventoEnergy2GoEntity e2g = eventoE2GRepository.findByEvento(evento);

        LocalDateTime agora = LocalDateTime.now();
        System.out.println("Agora: " + agora);
        System.out.println("Hora inicial: " + ufsm.getDataInicial());

        if (e2g != null) {
            e2g.setStatus(StatusEvento.INATIVO);
        }

        try {
            if (agora.isBefore(ufsm.getDataInicial())) {

                if (e2g != null) {

                    eventoE2GRepository.save(e2g);
                }

            } else {

                System.out.println("Entrou aqui");

                deletarEventoEnergy2Go();

                if (e2g != null) {
                    eventoE2GRepository.save(e2g);
                }
            }

        } catch (Exception e) {
            System.out.println("Erro ao deletar no repositório da energy: " + e.getMessage());
            throw new RuntimeException(e);
        }

        evento.setStatus(StatusEvento.INATIVO);
        eventoRepository.save(evento);

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
    public Double calculoPotenciaParaPorcentagem(EventoFrontDTO dto, Integer idInversor) {


        InversorEntity inversor = inversorRepository.findById(idInversor)
                .orElseThrow(() -> new RuntimeException("Inversor não encontrado"));

        Double potenciaInversor = calculoInversor(inversor.getId(), dto.value(), dto.type());

        Double porcentagemPotencia =0.0;

        if(dto.type()==TipoEvento.inject){

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

    public Double calcularPorcentagemPotencia(Double porcentagem, Integer idInversor, TipoEvento tipo) {


        System.out.println("Porcentagem entrada: "+ porcentagem);

        InversorEntity inversor = inversorRepository.findById(idInversor)
                .orElseThrow(() -> new RuntimeException("Inversor não encontrado"));

        Double potencia;

        if (tipo == TipoEvento.inject) {

            Double valorMaximoDescarga =
                    inversor.getPotenciaMaximaDescargaPorBateriaW() * inversor.getQuantidadeBaterias();

            potencia = (porcentagem * valorMaximoDescarga) / 100;


            if (potencia > valorMaximoDescarga) {
                potencia = valorMaximoDescarga;
            }

        } else {

            Double potenciaMaxima = inversor.getPotenciaMaximaW();

            potencia = (porcentagem * potenciaMaxima) / 100;


            if (potencia > potenciaMaxima) {
                potencia = potenciaMaxima;
            }
        }

        System.out.println("*** Porcentagem Potencia: " + potencia);
        return potencia;
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    //preciso do EventoFrontDTO -> pq faz as conversões dps em publicarDTOApiEnergy2Go
    public String agendarEventoEnergy(EventoFrontDTO dto){

        LocalDateTime dataInicialConvertida = Instant.ofEpochMilli(dto.startTime())
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        long atraso = Duration.between(LocalDateTime.now(), dataInicialConvertida).toMillis();

        if(atraso < 0){
            atraso = 0;
        }

        scheduler.schedule(() -> {
            try {
                publicarDTOApiEnergy2Go(dto);
            } catch (Exception e) {
                System.out.println("Erro ao executar evento Energy2Go: " + e.getMessage());
            }
        }, atraso, TimeUnit.MILLISECONDS);

        return "Evento agendado com sucesso";
    }


    public List<EventosUnificadosDTO> buscarEventosBanco(){


        List<EventoEntity> eventos = eventoRepository.findAll();

        List<EventosUnificadosDTO> dtos = new ArrayList<>();

        for (EventoEntity evento : eventos) {

            EventoUFSMEntity ufsm = eventoUFSMRepository.findByEvento(evento);
            boolean ufsmAtivo = ufsm != null && ufsm.getStatus() == StatusEvento.ATIVO;

            EventoEnergy2GoEntity e2g = eventoE2GRepository.findByEvento(evento);
            boolean energyAtivo = e2g != null && e2g.getStatus() == StatusEvento.ATIVO;


            if (!ufsmAtivo && !energyAtivo) {
                continue;
            }

            double potenciaUFSM = 0.0;
            double potenciaEnergy2Go = 0.0;
            String idAPI = null;

            if (ufsm != null) {

                idAPI = ufsm.getIdApi();

                if (ufsm.getStatus() == StatusEvento.ATIVO) {
                    potenciaUFSM = ufsm.getPotencia();
                }
            }

            if (energyAtivo) {
                potenciaEnergy2Go = e2g.getPotencia();
            }

            double total = potenciaUFSM + potenciaEnergy2Go;

            EventosUnificadosDTO unificado = new EventosUnificadosDTO(
                    evento.getId(),
                    total,
                    evento.getTipoEventoUFSM(),
                    evento.getDataInicial().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    evento.getDataFinal().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    idAPI,
                    evento.getStatus().toString()
            );

            dtos.add(unificado);
        }

        return dtos;
    }

    //sincronia a cada 30s -> pode apagar dados na api
    @Scheduled(fixedRate = 30000)
    public void sincronizar() {
        sincronizarAPI();
    }

    public void sincronizarAPI() {

        EventoFrontDTO[] eventosApi = buscarEventos();

        List<EventoUFSMEntity> eventosBanco = eventoUFSMRepository.findAll();

        Set<String> idsApi = Arrays.stream(eventosApi)
                .map(EventoFrontDTO::id)
                .collect(Collectors.toSet());

        LocalDateTime agora = LocalDateTime.now();

        for (EventoUFSMEntity banco : eventosBanco) {

            if (banco.getIdApi() == null) {
                continue;
            }

            boolean existeNaApi = idsApi.contains(banco.getIdApi());

            if (!existeNaApi && banco.getStatus() == StatusEvento.ATIVO) {

                System.out.println("Evento removido da API: " + banco.getIdApi());

                banco.setStatus(StatusEvento.INATIVO);
                eventoUFSMRepository.save(banco);
            }
        }
    }

//    public List<EventosUnificadosDTO> buscarEventosBanco(){
//
//
//        List<EventoEntity> eventos = eventoRepository.findAll();
//
//        List<EventosUnificadosDTO> dtos = new ArrayList<>();
//
//        for (EventoEntity evento : eventos) {
//
//            double potenciaUFSM = 0.0;
//            double potenciaEnergy2Go = 0.0;
//            String idAPI= null;
//
//
//            EventoUFSMEntity ufsm = eventoUFSMRepository.findByEvento(evento);
//            if (ufsm != null) {
//                potenciaUFSM = ufsm.getPotencia();
//                idAPI = ufsm.getIdApi();
//            }
//
//
//            EventoEnergy2GoEntity e2g = eventoE2GRepository.findByEvento(evento);
//
//            if (e2g != null) {
//                potenciaEnergy2Go = e2g.getPotencia();
//            }
//
//            double total = potenciaUFSM + potenciaEnergy2Go;
//
//            EventosUnificadosDTO unificado = new EventosUnificadosDTO(
//                    evento.getId(),
//                    total,
//                    evento.getTipoEventoUFSM(),
//                    evento.getDataInicial().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
//                    evento.getDataFinal().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
//                    idAPI,
//                    evento.getStatus().toString()
//
//            );
//
//            dtos.add(unificado);
//        }
//
//        return dtos;
//    }

}
