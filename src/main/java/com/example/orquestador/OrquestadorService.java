package com.example.orquestador;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class OrquestadorService {

    private final RestTemplate restTemplate;

    OrquestadorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final Logger logger = LoggerFactory.getLogger(OrquestadorService.class);

    private final String url = "http://localhost:8081/api/dominio/save";
    private final String urlError = "http://localhost:8080/api/dominio/save";
    
    private final BlockingQueue<RequestModel> retryQueue = new LinkedBlockingQueue<>();

    public void sendToDomain(RequestModel request) {
        if (request.getData() == null || request.getData().isEmpty()) {
            logger.error("Los datos no pueden estar vacíos");
            throw new IllegalArgumentException("Los datos no pueden estar vacíos");
        }

        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (HttpStatusCodeException e) {
            String errorMessage = "Error en la comunicación con el servicio de dominio: " + e.getStatusCode();
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        } catch (RestClientException e) {
            String errorMessage = "El servicio de dominio está fuera de servicio. Inténtelo más tarde.";
            logger.error(errorMessage);
            throw new RestClientException(errorMessage, e);
        }
    }

    public void sendToDomainError(RequestModel request) {
        if (request.getData() == null || request.getData().isEmpty()) {
            logger.error("Los datos no pueden estar vacíos");
            throw new IllegalArgumentException("Los datos no pueden estar vacíos");
        }

        restTemplate.postForEntity(urlError, request, String.class);
    }

    public void sendToDomainQueue(RequestModel request) {
        if (request.getData() == null || request.getData().isEmpty()) {
            logger.error("Los datos no pueden estar vacíos");
            throw new IllegalArgumentException("Los datos no pueden estar vacíos");
        }

        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (HttpStatusCodeException e) {
            String errorMessage = "Error en la comunicación con el servicio de dominio: " + e.getStatusCode();
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        } catch (RestClientException e) {
            retryQueue.add(request);
            logger.error("Dominio fuera de servicio. Datos almacenados en la cola para reintento.");
            throw new RuntimeException("Dominio fuera de servicio. Datos almacenados en la cola para reintento.", e);
        }
    }

    public void handleQueue() {
        
        BlockingQueue<RequestModel> temp = new LinkedBlockingQueue<>();
        while (!retryQueue.isEmpty()) {
            RequestModel request = retryQueue.poll();
            if (request != null) {
                try {
                    sendToDomain(request);
                } catch (Exception e) {
                    temp.add(request);
                    logger.error("Error al reintentar enviar los datos. Se reintentarán nuevamente.");
                }
            }
        }
        retryQueue.addAll(temp);
    }

    @Scheduled(fixedRate = 10000)
    public void attemptResending() {
        if (retryQueue.isEmpty()) {
            return;
        }
        logger.info("Reintentando envíos...");
        handleQueue();
        logger.info("Envíos restantes en cola: " + retryQueue.size());   
    }

}