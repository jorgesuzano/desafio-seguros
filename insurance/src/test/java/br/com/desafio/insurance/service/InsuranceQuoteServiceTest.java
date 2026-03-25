package br.com.desafio.insurance.service;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.PremiumAmountDTO;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.entity.QuoteStatus;
import br.com.desafio.insurance.domain.quote.CustomerDTO;
import br.com.desafio.insurance.domain.quote.CustomerType;
import br.com.desafio.insurance.domain.quote.Gender;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import br.com.desafio.insurance.persistence.repository.InsuranceQuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsuranceQuoteServiceTest {

    @Mock
    private InsuranceQuoteRepository quoteRepository;

    @InjectMocks
    private InsuranceQuoteService quoteService;

    private InsuranceQuoteRequestDTO validRequest;
    private OfferDTO validOffer;

    @BeforeEach
    void setUp() {
        Map<String, BigDecimal> coverages = new HashMap<>();
        coverages.put("Incêndio", new BigDecimal("250000.00"));
        coverages.put("Desastres naturais", new BigDecimal("500000.00"));
        coverages.put("Responsabiliadade civil", new BigDecimal("75000.00"));

        Map<String, BigDecimal> offerCoverages = new HashMap<>();
        offerCoverages.put("Incêndio", new BigDecimal("500000.00"));
        offerCoverages.put("Desastres naturais", new BigDecimal("600000.00"));
        offerCoverages.put("Responsabiliadade civil", new BigDecimal("80000.00"));

        CustomerDTO customer = CustomerDTO.builder()
                .documentNumber("36205578900")
                .name("John Wick")
                .type(CustomerType.NATURAL)
                .gender(Gender.MALE)
                .dateOfBirth("1973-05-02")
                .email("johnwick@gmail.com")
                .phoneNumber(11950503030L)
                .build();

        validRequest = InsuranceQuoteRequestDTO.builder()
                .productId("1b2da7cc-b367-4196-8a78-9cfeec21f587")
                .offerId("adc56d77-348c-4bf0-908f-22d402ee715c")
                .category("HOME")
                .totalMonthlyPremiumAmount(new BigDecimal("75.25"))
                .totalCoverageAmount(new BigDecimal("825000.00"))
                .coverages(coverages)
                .assistances(Arrays.asList("Encanador", "Eletricista", "Chaveiro 24h"))
                .customer(customer)
                .build();

        validOffer = OfferDTO.builder()
                .id("adc56d77-348c-4bf0-908f-22d402ee715c")
                .productId("1b2da7cc-b367-4196-8a78-9cfeec21f587")
                .name("Seguro de Vida Familiar")
                .active(true)
                .coverages(offerCoverages)
                .assistances(Arrays.asList("Encanador", "Eletricista", "Chaveiro 24h", "Assistência Funerária"))
                .monthlyPremiumAmount(PremiumAmountDTO.builder()
                        .minAmount(new BigDecimal("50.00"))
                        .maxAmount(new BigDecimal("100.74"))
                        .suggestedAmount(new BigDecimal("60.25"))
                        .build())
                .build();
    }

    @Nested
    @DisplayName("createAndValidateQuote")
    class CreateAndValidateQuote {

        @Test
        @DisplayName("should create quote successfully with valid data")
        void shouldCreateQuoteSuccessfully() {
            InsuranceQuote savedQuote = InsuranceQuote.builder()
                    .productId(validRequest.getProductId())
                    .offerId(validRequest.getOfferId())
                    .documentNumber("36205578900")
                    .status(QuoteStatus.RECEIVED)
                    .build();
            savedQuote.preWrite();

            when(quoteRepository.save(any(InsuranceQuote.class))).thenReturn(savedQuote);

            InsuranceQuote result = quoteService.createAndValidateQuote(validRequest, validOffer);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(QuoteStatus.RECEIVED);
            verify(quoteRepository).save(any(InsuranceQuote.class));
        }

        @Test
        @DisplayName("should throw exception when coverage not available in offer")
        void shouldThrowWhenCoverageNotInOffer() {
            validRequest.getCoverages().put("Cobertura Inexistente", new BigDecimal("10000.00"));

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cobertura 'Cobertura Inexistente' não está disponível");
        }

        @Test
        @DisplayName("should throw exception when coverage amount exceeds max allowed")
        void shouldThrowWhenCoverageExceedsMax() {
            validRequest.getCoverages().put("Incêndio", new BigDecimal("600000.00")); // max is 500000

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("excede o máximo permitido");
        }

        @Test
        @DisplayName("should throw exception when assistance not available in offer")
        void shouldThrowWhenAssistanceNotInOffer() {
            validRequest.setAssistances(Arrays.asList("Encanador", "Serviço Inexistente"));

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Assistência 'Serviço Inexistente' não está disponível");
        }

        @Test
        @DisplayName("should throw exception when monthly premium is below minimum")
        void shouldThrowWhenPremiumBelowMinimum() {
            validRequest.setTotalMonthlyPremiumAmount(new BigDecimal("10.00")); // min is 50.00

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("abaixo do mínimo permitido");
        }

        @Test
        @DisplayName("should throw exception when monthly premium exceeds maximum")
        void shouldThrowWhenPremiumExceedsMaximum() {
            validRequest.setTotalMonthlyPremiumAmount(new BigDecimal("200.00")); // max is 100.74

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("excede o máximo permitido");
        }

        @Test
        @DisplayName("should throw exception when total coverage amount does not match sum")
        void shouldThrowWhenTotalCoverageAmountMismatch() {
            validRequest.setTotalCoverageAmount(new BigDecimal("999999.00")); // actual sum is 825000

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("não corresponde ao valor informado");
        }

        @Test
        @DisplayName("should throw exception when coverages are empty")
        void shouldThrowWhenCoveragesEmpty() {
            validRequest.setCoverages(new HashMap<>());

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Pelo menos uma cobertura é obrigatória");
        }

        @Test
        @DisplayName("should throw exception when assistances are empty")
        void shouldThrowWhenAssistancesEmpty() {
            validRequest.setAssistances(java.util.Collections.emptyList());

            assertThatThrownBy(() -> quoteService.createAndValidateQuote(validRequest, validOffer))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Pelo menos uma assistência é obrigatória");
        }
    }
}

