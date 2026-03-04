package com.micromobilidade.vtn.vtn.service;

import com.micromobilidade.vtn.vtn.model.VtnDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class VtnService {

    private final RestClient restClient;
    private final String url;

    public VtnService(
            @Value("${username}") String username,
            @Value("${password}") String password,
            @Value("${url}") String url
    ) {


        this.url = url;

        this.restClient = RestClient.builder()
                .defaultHeaders(headers ->
                        headers.setBasicAuth(username, password)
                )
                .build();
    }


    public String publicarDTO(VtnDTO dto) {

        try {
            return restClient.post()
                    .uri(url + "/create")
                    .body(dto)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String buscarEventos(){

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
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
        VtnDTO [] eventos = restClient.get()
                .uri(url)
                .retrieve()
                .body(VtnDTO[].class);

        for(VtnDTO evento : eventos){
            long dataInicialDTO = evento.startTime();
            long dataFinalDTO = evento.endTime();

            boolean conflito = (dataInicial < dataFinalDTO) && (dataInicialDTO < dataFinal);

            if (conflito){
                return true;
            }
        }

        return false;
    }
}
