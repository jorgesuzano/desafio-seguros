package br.com.desafio.insurance.http.controller;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.PremiumAmountDTO;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.entity.QuoteStatus;
import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.messaging.producer.InsuranceQuoteProducer;
import br.com.desafio.insurance.service.CatalogValidationService;
import br.com.desafio.insurance.service.InsuranceQuoteService;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogValidationService catalogValidationService;

    @MockBean
    private InsuranceQuoteService quoteService;

    @MockBean
    private InsuranceQuoteProducer quoteProducer;

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
    @DisplayName("POST /api/v1/quotes - should return 201 Created with quote ID")
    void shouldCreateQuoteAndReturn201() throws Exception {
        when(catalogValidationService.validateProductAndOffer(anyString(), anyString()))
                .thenReturn(mockOffer);
        when(quoteService.createAndValidateQuote(any(), any())).thenReturn(savedQuote);
        doNothing().when(quoteProducer).publishQuoteReceivedEvent(any(InsuranceQuoteReceivedEvent.class));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(quoteProducer).publishQuoteReceivedEvent(any(InsuranceQuoteReceivedEvent.class));
    }

    @Test
    @DisplayName("POST /api/v1/quotes - should return 400 when catalog validation fails")
    void shouldReturn400WhenCatalogValidationFails() throws Exception {
        when(catalogValidationService.validateProductAndOffer(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Produto não encontrado"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Produto não encontrado"));
    }

    @Test
    @DisplayName("POST /api/v1/quotes - should return 400 when business validation fails")
    void shouldReturn400WhenBusinessValidationFails() throws Exception {
        when(catalogValidationService.validateProductAndOffer(anyString(), anyString()))
                .thenReturn(mockOffer);
        when(quoteService.createAndValidateQuote(any(), any()))
                .thenThrow(new IllegalArgumentException("Valor do prêmio inválido"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Valor do prêmio inválido"));
    }
}

