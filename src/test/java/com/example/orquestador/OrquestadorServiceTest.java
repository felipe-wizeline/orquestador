package com.example.orquestador;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class OrquestadorServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private OrquestadorService orquestadorService;

    private RequestModel validRequest;
    private RequestModel invalidRequest;

    @BeforeEach
    public void setUp() {
        validRequest = new RequestModel();
        validRequest.setData("Valid Data");

        invalidRequest = new RequestModel();
        invalidRequest.setData("{data: 'invalid'}");
    }

    @Test
    public void testSendToDomain_Success() {
        when(restTemplate.postForEntity(any(), any(), any())).thenReturn(null);
        orquestadorService.sendToDomain(validRequest);
        verify(restTemplate, times(1))
                .postForEntity(any(String.class), any(RequestModel.class), any(Class.class));
    }

    @Test
    public void testSendToDomainQueue_Success() {
        when(restTemplate.postForEntity(any(), any(), any())).thenReturn(null);
        orquestadorService.sendToDomainQueue(validRequest);
        verify(restTemplate, times(1))
                .postForEntity(any(String.class), any(RequestModel.class), any(Class.class));
    }
}