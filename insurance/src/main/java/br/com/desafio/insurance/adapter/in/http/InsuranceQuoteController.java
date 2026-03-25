package br.com.desafio.insurance.adapter.in.http;

import br.com.desafio.insurance.adapter.in.http.mapper.InsuranceQuoteResponseMapper;
import br.com.desafio.insurance.adapter.out.messaging.mapper.QuoteEventMapper;
import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.domain.exception.ServiceUnavailableException;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.port.in.InsuranceQuoteUseCase;
import br.com.desafio.insurance.domain.port.out.CatalogPort;
import br.com.desafio.insurance.domain.port.out.QuoteEventPublisherPort;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
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

    private final CatalogPort catalogPort;
    private final InsuranceQuoteUseCase quoteUseCase;
    private final QuoteEventPublisherPort quotePublisher;
    private final QuoteEventMapper eventMapper;
    private final InsuranceQuoteResponseMapper responseMapper;

    @PostMapping
    public ResponseEntity<?> createQuote(@RequestBody InsuranceQuoteRequestDTO request) {
        try {
            log.debug("Quote request received – product: {} offer: {}",
                    request.getProductId(), request.getOfferId());

            OfferDTO validatedOffer = catalogPort.validateProductAndOffer(
                    request.getProductId(), request.getOfferId());

            InsuranceQuote quote = quoteUseCase.createAndValidateQuote(request, validatedOffer);

            InsuranceQuoteReceivedEvent event = eventMapper.toEvent(quote, request);
            quotePublisher.publish(event);

            Map<String, Object> response = new HashMap<>();
            response.put("id",     quote.getId());
            response.put("status", quote.getStatus().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Quote validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(errorBody(e.getMessage()));
        } catch (ServiceUnavailableException e) {
            log.warn("Dependency temporarily unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
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
            InsuranceQuote quote = quoteUseCase.getQuoteById(quoteId, documentNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Cotação não encontrada: " + quoteId));

            return ResponseEntity.ok(responseMapper.toDTO(quote));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e.getMessage()));
        } catch (ServiceUnavailableException e) {
            log.warn("Dependency temporarily unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorBody(e.getMessage()));
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

