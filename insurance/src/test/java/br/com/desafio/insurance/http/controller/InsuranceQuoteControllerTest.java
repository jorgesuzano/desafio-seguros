package br.com.desafio.insurance.http.controller;

import br.com.desafio.insurance.adapter.in.http.InsuranceQuoteController;
import br.com.desafio.insurance.adapter.in.http.dto.InsuranceQuoteResponseDTO;
import br.com.desafio.insurance.adapter.in.http.mapper.InsuranceQuoteResponseMapper;
import br.com.desafio.insurance.adapter.out.messaging.mapper.QuoteEventMapper;
import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.PremiumAmountDTO;
import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.domain.exception.ServiceUnavailableException;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.model.QuoteStatus;
import br.com.desafio.insurance.domain.port.in.InsuranceQuoteUseCase;
import br.com.desafio.insurance.domain.port.out.CatalogPort;
import br.com.desafio.insurance.domain.port.out.QuoteEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link InsuranceQuoteController}.
 *
 * <p>Strategy: {@code @WebMvcTest} loads only the HTTP slice; every
 * service/port dependency is replaced by a Mockito mock.
 *
 * <p>Why no {@code @Nested}: Spring Boot creates a <em>separate</em> application
 * context per nested test class.  The {@code @TestConfiguration} defined here
 * would not be included in those child contexts, causing the
 * {@code MeterRegistry} bean to be absent and the context load to fail.
 * Keeping all tests in the same class guarantees a single shared context.
 *
 * <p>Coverage:
 * <ul>
 *   <li>POST /api/v1/quotes  → 201, 422 (catalog), 422 (business), 503, 500 (x2)</li>
 *   <li>GET  /api/v1/quotes/{id}/{doc} → 200, 404, 503, 500</li>
 * </ul>
 */
@WebMvcTest(InsuranceQuoteController.class)
@DisplayName("InsuranceQuoteController")
class InsuranceQuoteControllerTest {

    /**
     * Provides a lightweight {@link SimpleMeterRegistry} so that
     * {@link InsuranceQuoteController} (which requires a {@link MeterRegistry}
     * via {@code @RequiredArgsConstructor}) can be instantiated.
     *
     * <p>{@code @WebMvcTest} disables observability auto-configuration by
     * default ({@code DisableObservabilityContextCustomizer}), so no registry
     * is present unless explicitly supplied here.
     */
    @TestConfiguration
    static class MetricsTestConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    // ── Infrastructure ─────────────────────────────────────────────────────
    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    // ── Mocked dependencies ────────────────────────────────────────────────
    @MockBean CatalogPort               catalogPort;
    @MockBean InsuranceQuoteUseCase     quoteUseCase;
    @MockBean QuoteEventPublisherPort   quotePublisher;
    @MockBean QuoteEventMapper          eventMapper;
    @MockBean InsuranceQuoteResponseMapper responseMapper;

    // ── Shared fixtures ────────────────────────────────────────────────────
    private static final String PRODUCT_ID = "1b2da7cc-b367-4196-8a78-9cfeec21f587";
    private static final String OFFER_ID   = "adc56d77-348c-4bf0-908f-22d402ee715c";
    private static final String QUOTE_ID   = "quote-abc-123";
    private static final String DOC_NUMBER = "36205578900";

    private Map<String, Object> validRequest;
    private OfferDTO            mockOffer;
    private InsuranceQuote      savedQuote;

    @BeforeEach
    void setUp() {
        Map<String, BigDecimal> offerCoverages = new HashMap<>();
        offerCoverages.put("Incêndio",                 new BigDecimal("500000.00"));
        offerCoverages.put("Desastres naturais",        new BigDecimal("600000.00"));
        offerCoverages.put("Responsabiliadade civil",   new BigDecimal("80000.00"));

        mockOffer = OfferDTO.builder()
                .id(OFFER_ID).productId(PRODUCT_ID)
                .name("Seguro de Vida Familiar").active(true)
                .coverages(offerCoverages)
                .assistances(List.of("Encanador", "Eletricista", "Chaveiro 24h"))
                .monthlyPremiumAmount(PremiumAmountDTO.builder()
                        .minAmount(new BigDecimal("50.00"))
                        .maxAmount(new BigDecimal("100.74"))
                        .suggestedAmount(new BigDecimal("60.25"))
                        .build())
                .build();

        savedQuote = InsuranceQuote.builder()
                .productId(PRODUCT_ID).offerId(OFFER_ID)
                .documentNumber(DOC_NUMBER).status(QuoteStatus.RECEIVED)
                .build();
        savedQuote.preWrite();

        Map<String, Object> customer = Map.of(
                "document_number", DOC_NUMBER,
                "name",            "John Wick",
                "type",            "NATURAL",
                "gender",          "MALE",
                "date_of_birth",   "1973-05-02",
                "email",           "johnwick@gmail.com",
                "phone_number",    11950503030L
        );

        Map<String, Object> coverages = Map.of(
                "Incêndio",                250000.00,
                "Desastres naturais",       500000.00,
                "Responsabiliadade civil",   75000.00
        );

        validRequest = new HashMap<>();
        validRequest.put("product_id",                   PRODUCT_ID);
        validRequest.put("offer_id",                     OFFER_ID);
        validRequest.put("category",                     "HOME");
        validRequest.put("total_monthly_premium_amount", 75.25);
        validRequest.put("total_coverage_amount",        825000.00);
        validRequest.put("coverages",                    coverages);
        validRequest.put("assistances",                  List.of("Encanador", "Eletricista", "Chaveiro 24h"));
        validRequest.put("customer",                     customer);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST /api/v1/quotes
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST 201 – quote criada, evento publicado, resposta contém id e status")
    void post_shouldCreateQuoteAndReturn201() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString())).thenReturn(mockOffer);
        when(quoteUseCase.createAndValidateQuote(any(), any())).thenReturn(savedQuote);
        when(eventMapper.toEvent(any(), any())).thenReturn(InsuranceQuoteReceivedEvent.builder().build());
        doNothing().when(quotePublisher).publish(any());

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(catalogPort).validateProductAndOffer(anyString(), anyString());
        verify(quoteUseCase).createAndValidateQuote(any(), any());
        verify(quotePublisher).publish(any());
    }

    @Test
    @DisplayName("POST 422 – produto não encontrado no catálogo (IllegalArgumentException)")
    void post_shouldReturn422WhenCatalogProductNotFound() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Produto não encontrado"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Produto não encontrado"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(quoteUseCase, never()).createAndValidateQuote(any(), any());
        verify(quotePublisher, never()).publish(any());
    }

    @Test
    @DisplayName("POST 422 – validação de negócio falha no service (prêmio fora do intervalo)")
    void post_shouldReturn422WhenBusinessValidationFails() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString())).thenReturn(mockOffer);
        when(quoteUseCase.createAndValidateQuote(any(), any()))
                .thenThrow(new IllegalArgumentException("Valor do prêmio inválido"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Valor do prêmio inválido"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(quotePublisher, never()).publish(any());
    }

    @Test
    @DisplayName("POST 503 – circuit breaker ABERTO no catálogo (ServiceUnavailableException)")
    void post_shouldReturn503WhenCatalogCircuitBreakerIsOpen() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString()))
                .thenThrow(new ServiceUnavailableException(
                        "Catálogo de produtos temporariamente indisponível. Tente novamente em instantes."));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error")
                        .value("Catálogo de produtos temporariamente indisponível. Tente novamente em instantes."));

        verify(quotePublisher, never()).publish(any());
    }

    @Test
    @DisplayName("POST 503 – circuit breaker ABERTO no DynamoDB (ServiceUnavailableException no save)")
    void post_shouldReturn503WhenPersistenceCircuitBreakerIsOpen() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString())).thenReturn(mockOffer);
        when(quoteUseCase.createAndValidateQuote(any(), any()))
                .thenThrow(new ServiceUnavailableException("Banco de dados temporariamente indisponível"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Banco de dados temporariamente indisponível"));

        verify(quotePublisher, never()).publish(any());
    }

    @Test
    @DisplayName("POST 500 – RuntimeException inesperada na camada de serviço")
    void post_shouldReturn500OnUnexpectedServiceError() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString())).thenReturn(mockOffer);
        when(quoteUseCase.createAndValidateQuote(any(), any()))
                .thenThrow(new RuntimeException("Unexpected internal failure"));

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro ao processar cotação"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST 500 – publicação do evento falha (SQS connection refused)")
    void post_shouldReturn500WhenEventPublishingThrows() throws Exception {
        when(catalogPort.validateProductAndOffer(anyString(), anyString())).thenReturn(mockOffer);
        when(quoteUseCase.createAndValidateQuote(any(), any())).thenReturn(savedQuote);
        when(eventMapper.toEvent(any(), any())).thenReturn(InsuranceQuoteReceivedEvent.builder().build());
        doThrow(new RuntimeException("SQS connection refused")).when(quotePublisher).publish(any());

        mockMvc.perform(post("/api/v1/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro ao processar cotação"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/v1/quotes/{quoteId}/{documentNumber}
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET 200 – retorna DTO completo quando a cotação existe")
    void get_shouldReturnQuoteWith200() throws Exception {
        InsuranceQuoteResponseDTO dto = InsuranceQuoteResponseDTO.builder()
                .id(QUOTE_ID).productId(PRODUCT_ID).offerId(OFFER_ID).category("HOME")
                .build();

        when(quoteUseCase.getQuoteById(QUOTE_ID, DOC_NUMBER)).thenReturn(Optional.of(savedQuote));
        when(responseMapper.toDTO(savedQuote)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/quotes/{quoteId}/{documentNumber}", QUOTE_ID, DOC_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUOTE_ID))
                .andExpect(jsonPath("$.product_id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.offer_id").value(OFFER_ID))
                .andExpect(jsonPath("$.category").value("HOME"));

        verify(quoteUseCase).getQuoteById(QUOTE_ID, DOC_NUMBER);
        verify(responseMapper).toDTO(savedQuote);
    }

    @Test
    @DisplayName("GET 404 – cotação não encontrada para o par id/documento")
    void get_shouldReturn404WhenQuoteNotFound() throws Exception {
        when(quoteUseCase.getQuoteById(QUOTE_ID, DOC_NUMBER)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/quotes/{quoteId}/{documentNumber}", QUOTE_ID, DOC_NUMBER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cotação não encontrada: " + QUOTE_ID))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(responseMapper, never()).toDTO(any());
    }

    @Test
    @DisplayName("GET 503 – circuit breaker ABERTO no DynamoDB durante consulta")
    void get_shouldReturn503WhenServiceUnavailableDuringGet() throws Exception {
        when(quoteUseCase.getQuoteById(anyString(), anyString()))
                .thenThrow(new ServiceUnavailableException("Banco de dados temporariamente indisponível"));

        mockMvc.perform(get("/api/v1/quotes/{quoteId}/{documentNumber}", QUOTE_ID, DOC_NUMBER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Banco de dados temporariamente indisponível"));

        verify(responseMapper, never()).toDTO(any());
    }

    @Test
    @DisplayName("GET 500 – RuntimeException inesperada durante consulta")
    void get_shouldReturn500OnUnexpectedErrorDuringGet() throws Exception {
        when(quoteUseCase.getQuoteById(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection pool exhausted"));

        mockMvc.perform(get("/api/v1/quotes/{quoteId}/{documentNumber}", QUOTE_ID, DOC_NUMBER))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro ao recuperar cotação"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(responseMapper, never()).toDTO(any());
    }
}
