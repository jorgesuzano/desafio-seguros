package br.com.desafio.insurance.http.controller;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import br.com.desafio.insurance.messaging.mapper.QuoteEventMapper;
import br.com.desafio.insurance.messaging.producer.InsuranceQuoteProducer;
import br.com.desafio.insurance.service.CatalogPort;
import br.com.desafio.insurance.service.InsuranceQuoteServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class InsuranceQuoteController {

    /** Depends on interfaces, not implementations (DIP). */
    private final CatalogPort catalogPort;
    private final InsuranceQuoteServicePort quoteService;
    private final InsuranceQuoteProducer quoteProducer;
    private final QuoteEventMapper eventMapper;
    private final InsuranceQuoteResponseMapper responseMapper;

    @PostMapping
    public ResponseEntity<?> createQuote(@RequestBody InsuranceQuoteRequestDTO request) {
        try {
            log.debug("Quote request received – product: {} offer: {}",
                    request.getProductId(), request.getOfferId());

            OfferDTO validatedOffer = catalogPort.validateProductAndOffer(
                    request.getProductId(), request.getOfferId());

            InsuranceQuote quote = quoteService.createAndValidateQuote(request, validatedOffer);

            InsuranceQuoteReceivedEvent event = eventMapper.toEvent(quote, request);
            quoteProducer.publishQuoteReceivedEvent(event);

            Map<String, Object> response = new HashMap<>();
            response.put("id",     quote.getId());
            response.put("status", quote.getStatus().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Quote validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(errorBody(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Erro ao processar cotação"));
        }
    }

    @GetMapping("/{quoteId}/{documentNumber}")
    public ResponseEntity<?> getQuote(@PathVariable String quoteId,
                                      @PathVariable String documentNumber) {
        try {
            InsuranceQuote quote = quoteService.getQuoteById(quoteId, documentNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Cotação não encontrada: " + quoteId));

            return ResponseEntity.ok(responseMapper.toDTO(quote));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving quote {}", quoteId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Erro ao recuperar cotação"));
        }
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error",     message);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }
}
