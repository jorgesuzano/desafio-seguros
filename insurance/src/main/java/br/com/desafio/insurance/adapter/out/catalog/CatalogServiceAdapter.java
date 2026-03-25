package br.com.desafio.insurance.adapter.out.catalog;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.ProductDTO;
import br.com.desafio.insurance.domain.exception.ServiceUnavailableException;
import br.com.desafio.insurance.domain.port.out.CatalogPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogServiceAdapter implements CatalogPort {

    private static final String CB_NAME = "catalogService";

    private final CatalogServiceClient catalogServiceClient;
    private final MeterRegistry meterRegistry;

    @Override
    @Retry(name = CB_NAME)
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "catalogFallback")
    public OfferDTO validateProductAndOffer(String productId, String offerId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ProductDTO product = catalogServiceClient.getProduct(productId);
            validateProduct(productId, product);

            OfferDTO offer = catalogServiceClient.getOffer(offerId);
            validateOffer(offerId, productId, offer);

            log.debug("Product {} and offer {} validated", productId, offerId);

            // ── outbound call succeeded ───────────────────────────────────────
            meterRegistry.counter("insurance.catalog.calls.total",
                    "outcome", "success").increment();

            return offer;

        } catch (IllegalArgumentException e) {
            // Business validation error — count separately, not as infra failure
            meterRegistry.counter("insurance.catalog.calls.total",
                    "outcome", "validation_error").increment();
            throw e;

        } finally {
            sample.stop(Timer.builder("insurance.catalog.call.duration")
                    .description("Latency of outbound calls to the Catalog service")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
        }
    }

    // ---- fallback -------------------------------------------------------

    @SuppressWarnings("unused")
    private OfferDTO catalogFallback(String productId, String offerId, Throwable ex) {
        log.error("Circuit breaker OPEN for catalog service – productId: {} offerId: {} cause: {}",
                productId, offerId, ex.getMessage());

        // ── metric: circuit-breaker triggered ────────────────────────────────
        meterRegistry.counter("insurance.catalog.calls.total",
                "outcome", "circuit_open").increment();

        throw new ServiceUnavailableException(
                "Catálogo de produtos temporariamente indisponível. Tente novamente em instantes.", ex);
    }

    // ---- private helpers ------------------------------------------------

    private void validateProduct(String productId, ProductDTO product) {
        if (product == null)
            throw new IllegalArgumentException("Produto não encontrado: " + productId);
        if (!Boolean.TRUE.equals(product.getActive()))
            throw new IllegalArgumentException("Produto inativo: " + productId);
    }

    private void validateOffer(String offerId, String productId, OfferDTO offer) {
        if (offer == null)
            throw new IllegalArgumentException("Oferta não encontrada: " + offerId);
        if (!Boolean.TRUE.equals(offer.getActive()))
            throw new IllegalArgumentException("Oferta inativa: " + offerId);
        if (!productId.equals(offer.getProductId()))
            throw new IllegalArgumentException("Oferta não pertence ao produto informado");
    }
}
