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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final MeterRegistry meterRegistry;

    @PostMapping
    public ResponseEntity<?> createQuote(@RequestBody InsuranceQuoteRequestDTO request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
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

            // ── business metric: quote successfully created ──────────────────
            meterRegistry.counter("insurance.quote.created.total",
                    "product_id", request.getProductId()).increment();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            outcome = "validation_error";
            log.warn("Quote validation failed: {}", e.getMessage());

            // ── business metric: validation failure ──────────────────────────
            meterRegistry.counter("insurance.quote.validation.error.total",
                    "reason", sanitize(e.getMessage())).increment();

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(errorBody(e.getMessage()));

        } catch (ServiceUnavailableException e) {
            outcome = "service_unavailable";
            log.warn("Dependency temporarily unavailable: {}", e.getMessage());

            // ── business metric: circuit-breaker / dependency down ────────────
            meterRegistry.counter("insurance.service.unavailable.total",
                    "dependency", "catalog").increment();

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorBody(e.getMessage()));

        } catch (Exception e) {
            outcome = "error";
            log.error("Unexpected error creating quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Erro ao processar cotação"));

        } finally {
            // ── latency metric with p50/p95/p99 percentiles ───────────────────
            sample.stop(Timer.builder("insurance.quote.creation.duration")
                    .description("End-to-end latency of POST /api/v1/quotes")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
        }
    }

    @GetMapping("/{quoteId}/{documentNumber}")
    public ResponseEntity<?> getQuote(@PathVariable String quoteId,
                                      @PathVariable String documentNumber) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            InsuranceQuote quote = quoteUseCase.getQuoteById(quoteId, documentNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Cotação não encontrada: " + quoteId));

            return ResponseEntity.ok(responseMapper.toDTO(quote));

        } catch (IllegalArgumentException e) {
            outcome = "not_found";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(e.getMessage()));

        } catch (ServiceUnavailableException e) {
            outcome = "service_unavailable";
            log.warn("Dependency temporarily unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorBody(e.getMessage()));

        } catch (Exception e) {
            outcome = "error";
            log.error("Error retrieving quote {}", quoteId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Erro ao recuperar cotação"));

        } finally {
            sample.stop(Timer.builder("insurance.quote.get.duration")
                    .description("Latency of GET /api/v1/quotes/{id}/{doc}")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
        }
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error",     message);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }

    /** Truncate long error messages to avoid high-cardinality tag values. */
    private String sanitize(String msg) {
        if (msg == null) return "unknown";
        return msg.length() > 80 ? msg.substring(0, 80) : msg;
    }
}

