package br.com.desafio.insurance.http.controller;

import br.com.desafio.insurance.adapter.in.http.InsuranceQuoteController;
import br.com.desafio.insurance.adapter.in.http.mapper.InsuranceQuoteResponseMapper;
import br.com.desafio.insurance.adapter.out.messaging.mapper.QuoteEventMapper;
import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.PremiumAmountDTO;
import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.model.QuoteStatus;
import br.com.desafio.insurance.domain.port.in.InsuranceQuoteUseCase;
import br.com.desafio.insurance.domain.port.out.CatalogPort;
import br.com.desafio.insurance.domain.port.out.QuoteEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsuranceQuoteController.class)
class InsuranceQuoteControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CatalogPort catalogPort;
    @MockBean InsuranceQuoteUseCase quoteUseCase;
    @MockBean QuoteEventPublisherPort quotePublisher;
    @MockBean QuoteEventMapper eventMapper;
    @MockBean InsuranceQuoteResponseMapper responseMapper;

    private Map<String, Object> validRequestBody;
    private OfferDTO mockOffer;
    private InsuranceQuote savedQuote;

    @BeforeEach
    void setUp() {
        Map<String, BigDecimal> offerCoverages = new HashMap<>();
        offerCoverages.put("Incêndio", new BigDecimal("500000.00"));
        offerCoverages.put("Desastres naturais", new BigDecimal("600000.00"));
        offerCoverages.put("Responsabiliadade civil", new BigDecimal("80000.00"));

        mockOffer = OfferDTO.builder()
                .id("adc56d77-348c-4bf0-908f-22d402ee715c")
                .productId("1b2da7cc-b367-4196-8a78-9cfeec21f587")
                .name("Seguro de Vida Familiar")
                .active(true)
                .coverages(offerCoverages)
                .assistances(Arrays.asList("Encanador", "Eletricista", "Chaveiro 24h"))
                .monthlyPremiumAmount(PremiumAmountDTO.builder()
                        .minAmount(new BigDecimal("50.00"))
                        .maxAmount(new BigDecimal("100.74"))
                        .suggestedAmount(new BigDecimal("60.25"))
                        .build())
                .build();

        savedQuote = InsuranceQuote.builder()
                .productId("1b2da7cc-b367-4196-8a78-9cfeec21f587")
                .offerId("adc56d77-348c-4bf0-908f-22d402ee715c")
                .documentNumber("36205578900")
                .status(QuoteStatus.RECEIVED)
                .build();
        savedQuote.preWrite();

        Map<String, Object> customer = new HashMap<>();
        customer.put("document_number", "36205578900");
        customer.put("name", "John Wick");
        customer.put("type", "NATURAL");
        customer.put("gender", "MALE");
        customer.put("date_of_birth", "1973-05-02");
        customer.put("email", "johnwick@gmail.com");
        customer.put("phone_number", 11950503030L);

        Map<String, Object> coverages = new HashMap<>();
        coverages.put("Incêndio", 250000.00);
        coverages.put("Desastres naturais", 500000.00);
        coverages.put("Responsabiliadade civil", 75000.00);

        validRequestBody = new HashMap<>();
        validRequestBody.put("product_id", "1b2da7cc-b367-4196-8a78-9cfeec21f587");
        validRequestBody.put("offer_id", "adc56d77-348c-4bf0-908f-22d402ee715c");
        validRequestBody.put("category", "HOME");
        validRequestBody.put("total_monthly_premium_amount", 75.25);
        validRequestBody.put("total_coverage_amount", 825000.00);
        validRequestBody.put("coverages", coverages);
        validRequestBody.put("assistances", Arrays.asList("Encanador", "Eletricista", "Chaveiro 24h"));
        validRequestBody.put("customer", customer);
    }

    @Test
    @DisplayName("POST /api/v1/quotes - should return 201 Created")
    void shouldCreateQuoteAndReturn201() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString())).thenReturn(mockOffer);
        when(quoteUseCase.createAndValidateQuote(any(), any())).thenReturn(savedQuote);
        when(eventMapper.toEvent(any(), any())).thenReturn(InsuranceQuoteReceivedEvent.builder().build());
        doNothing().when(quotePublisher).publish(any());

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(quotePublisher).publish(any());
    }

    @Test
    @DisplayName("POST /api/v1/quotes - should return 422 when catalog validation fails")
    void shouldReturn422WhenCatalogValidationFails() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Produto não encontrado"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestBody)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Produto não encontrado"));
    }

    @Test
    @DisplayName("POST /api/v1/quotes - should return 422 when business validation fails")
    void shouldReturn422WhenBusinessValidationFails() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString())).thenReturn(mockOffer);
        when(quoteUseCase.createAndValidateQuote(any(), any()))
                .thenThrow(new IllegalArgumentException("Valor do prêmio inválido"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestBody)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Valor do prêmio inválido"));
    }
}

