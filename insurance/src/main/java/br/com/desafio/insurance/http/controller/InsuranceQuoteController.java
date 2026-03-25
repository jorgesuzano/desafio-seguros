package br.com.desafio.insurance.http.controller;

import br.com.desafio.insurance.domain.quote.CustomerDTO;
import br.com.desafio.insurance.service.CatalogValidationService;
import br.com.desafio.insurance.service.InsuranceQuoteService;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import br.com.desafio.insurance.domain.dto.InsuranceQuoteResponseDTO;
import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.messaging.producer.InsuranceQuoteProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class InsuranceQuoteController {

    private final CatalogValidationService catalogValidationService;
    private final InsuranceQuoteService quoteService;
    private final InsuranceQuoteProducer quoteProducer;

    @PostMapping
    public ResponseEntity<?> createQuote(@Valid @RequestBody InsuranceQuoteRequestDTO request) {
        try {
            log.info("Received insurance quote request for product: {} and offer: {}",
                    request.getProductId(), request.getOfferId());

            // 1. Validar produto e oferta contra o catálogo
            OfferDTO validatedOffer = catalogValidationService
                    .validateProductAndOffer(request.getProductId(), request.getOfferId());

            log.info("Quote validated successfully against catalog. Offer: {}", validatedOffer.getName());

            // 2. Criar e persistir cotação com validações
//            InsuranceQuote quote = quoteService.createAndValidateQuote(request, validatedOffer);
//
//            // 3. Publicar evento Kafka
//            Map<String, Object> customerMap = new HashMap<>();
//            customerMap.put("document_number", request.getCustomer().getDocumentNumber());
//            customerMap.put("name", request.getCustomer().getName());
//            customerMap.put("type", request.getCustomer().getType().toString());
//            customerMap.put("gender", request.getCustomer().getGender().toString());
//            customerMap.put("date_of_birth", request.getCustomer().getDateOfBirth());
//            customerMap.put("email", request.getCustomer().getEmail());
//            customerMap.put("phone_number", request.getCustomer().getPhoneNumber());
//
//            InsuranceQuoteReceivedEvent event = InsuranceQuoteReceivedEvent.builder()
//                    .quoteId(quote.getId())
//                    .productId(request.getProductId())
//                    .offerId(request.getOfferId())
//                    .category(request.getCategory())
//                    .totalMonthlyPremiumAmount(request.getTotalMonthlyPremiumAmount())
//                    .totalCoverageAmount(request.getTotalCoverageAmount())
//                    .coverages(request.getCoverages())
//                    .assistances(request.getAssistances())
//                    .customer(customerMap)
//                    .receivedAt(LocalDateTime.now())
//                    .build();
//
//            quoteProducer.publishQuoteReceivedEvent(event);

            // 4. Retornar resposta com ID
//            log.info("Quote processed successfully with ID: {}", quote.getId());

            Map<String, Object> response = new HashMap<>();
//            response.put("id", quote.getId());
//            response.put("message", "Cotação criada com sucesso");
//            response.put("status", quote.getStatus().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            log.error("Error processing quote", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erro ao processar cotação: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{quoteId}/{documentNumber}")
    public ResponseEntity<?> getQuote(
            @PathVariable String quoteId,
            @PathVariable String documentNumber) {
        try {
            log.info("Fetching quote with ID: {} and documentNumber: {}", quoteId, documentNumber);

            InsuranceQuote quote = quoteService.getQuoteById(quoteId, documentNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + quoteId));

            InsuranceQuoteResponseDTO response = InsuranceQuoteResponseDTO.builder()
                    .id(quote.getId())
                    .insurancePolicyId(quote.getInsurancePolicyId())
                    .productId(quote.getProductId())
                    .offerId(quote.getOfferId())
                    .category(quote.getCategory())
                    .createdAt(convertTimestampToLocalDateTime(quote.getCreatedAt()))
                    .updatedAt(convertTimestampToLocalDateTime(quote.getUpdatedAt()))
                    .totalMonthlyPremiumAmount(quote.getTotalMonthlyPremiumAmount())
                    .totalCoverageAmount(quote.getTotalCoverageAmount())
                    .coverages(quote.getCoverages())
                    .assistances(quote.getAssistances())
                    .customer(CustomerDTO.builder()
                            .documentNumber(quote.getCustomer().getDocumentNumber())
                            .name(quote.getCustomer().getName())
                            .type(quote.getCustomer().getType())
                            .gender(quote.getCustomer().getGender())
                            .dateOfBirth(quote.getCustomer().getDateOfBirth())
                            .email(quote.getCustomer().getEmail())
                            .phoneNumber(quote.getCustomer().getPhoneNumber())
                            .build())
                    .build();

            log.info("Quote retrieved successfully with ID: {}", quoteId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Quote not found: {}", quoteId);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error retrieving quote", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erro ao recuperar cotação: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private LocalDateTime convertTimestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
        );
    }
}

