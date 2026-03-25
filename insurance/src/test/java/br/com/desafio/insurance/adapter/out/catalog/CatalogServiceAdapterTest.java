package br.com.desafio.insurance.adapter.out.catalog;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.ProductDTO;
import br.com.desafio.insurance.domain.exception.ServiceUnavailableException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CatalogServiceAdapter}.
 *
 * <p>Strategy: plain Mockito — no Spring context, no AOP.
 * Consequence: {@code @CircuitBreaker} and {@code @Retry} annotations are
 * <em>not</em> active; the fallback method is tested directly via reflection.
 * Integration-level circuit-breaker behaviour belongs in a separate
 * {@code @SpringBootTest} integration test.
 *
 * <p>A real {@link SimpleMeterRegistry} is used instead of a mock to avoid NPE
 * inside {@code Timer.start()} and to allow counter/timer assertions.
 *
 * <p>Coverage:
 * <ul>
 *   <li>Happy path – valid product + valid offer → returns OfferDTO</li>
 *   <li>Product validation – null, inactive (false), active = null</li>
 *   <li>Offer validation – null, inactive, active = null, wrong product</li>
 *   <li>Metrics – success / validation_error / circuit_open counters + timer</li>
 *   <li>Fallback – ServiceUnavailableException + circuit_open counter</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogServiceAdapter")
class CatalogServiceAdapterTest {

    @Mock
    private CatalogServiceClient catalogServiceClient;

    // Real registry — avoids NPE inside Timer.start() / sample.stop()
    // and enables asserting counter / timer values.
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private CatalogServiceAdapter adapter;

    // ── Constants ──────────────────────────────────────────────────────────
    private static final String PRODUCT_ID       = "1b2da7cc-b367-4196-8a78-9cfeec21f587";
    private static final String OFFER_ID         = "adc56d77-348c-4bf0-908f-22d402ee715c";
    private static final String OTHER_PRODUCT_ID = "other-product-id-xyz";

    @BeforeEach
    void setUp() {
        adapter = new CatalogServiceAdapter(catalogServiceClient, meterRegistry);
    }

    // ── Fixtures ───────────────────────────────────────────────────────────

    private ProductDTO activeProduct() {
        return ProductDTO.builder()
                .id(PRODUCT_ID)
                .name("Seguro de Vida")
                .active(true)
                .offers(List.of(OFFER_ID))
                .build();
    }

    private OfferDTO activeOffer() {
        return OfferDTO.builder()
                .id(OFFER_ID)
                .productId(PRODUCT_ID)
                .name("Seguro de Vida Familiar")
                .active(true)
                .coverages(Map.of("Incêndio", new BigDecimal("500000.00")))
                .assistances(List.of("Encanador", "Eletricista"))
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Happy path
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("deve retornar OfferDTO quando produto e oferta são válidos e relacionados")
    void shouldReturnOfferWhenProductAndOfferAreValid() {
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(activeProduct());
        when(catalogServiceClient.getOffer(OFFER_ID)).thenReturn(activeOffer());

        OfferDTO result = adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(OFFER_ID);
        assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getActive()).isTrue();

        verify(catalogServiceClient).getProduct(PRODUCT_ID);
        verify(catalogServiceClient).getOffer(OFFER_ID);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Product validation
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando produto retorna null (não encontrado)")
    void shouldThrowWhenProductIsNull() {
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Produto não encontrado: " + PRODUCT_ID);

        // oferta NÃO deve ser consultada quando o produto falha
        verify(catalogServiceClient, never()).getOffer(anyString());
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando produto está inativo (active=false)")
    void shouldThrowWhenProductIsInactive() {
        ProductDTO inactiveProduct = ProductDTO.builder()
                .id(PRODUCT_ID).name("Seguro de Vida").active(false).build();
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(inactiveProduct);

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Produto inativo: " + PRODUCT_ID);

        verify(catalogServiceClient, never()).getOffer(anyString());
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando produto tem active=null")
    void shouldThrowWhenProductActiveIsNull() {
        ProductDTO nullActiveProduct = ProductDTO.builder()
                .id(PRODUCT_ID).name("Seguro de Vida").active(null).build();
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(nullActiveProduct);

        // Boolean.TRUE.equals(null) == false, portanto entra no branch "inativo"
        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Produto inativo: " + PRODUCT_ID);

        verify(catalogServiceClient, never()).getOffer(anyString());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Offer validation
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando oferta retorna null (não encontrada)")
    void shouldThrowWhenOfferIsNull() {
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(activeProduct());
        when(catalogServiceClient.getOffer(OFFER_ID)).thenReturn(null);

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Oferta não encontrada: " + OFFER_ID);
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando oferta está inativa (active=false)")
    void shouldThrowWhenOfferIsInactive() {
        OfferDTO inactiveOffer = OfferDTO.builder()
                .id(OFFER_ID).productId(PRODUCT_ID).active(false).build();
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(activeProduct());
        when(catalogServiceClient.getOffer(OFFER_ID)).thenReturn(inactiveOffer);

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Oferta inativa: " + OFFER_ID);
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando oferta tem active=null")
    void shouldThrowWhenOfferActiveIsNull() {
        OfferDTO nullActiveOffer = OfferDTO.builder()
                .id(OFFER_ID).productId(PRODUCT_ID).active(null).build();
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(activeProduct());
        when(catalogServiceClient.getOffer(OFFER_ID)).thenReturn(nullActiveOffer);

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Oferta inativa: " + OFFER_ID);
    }

    @Test
    @DisplayName("deve lançar IllegalArgumentException quando oferta pertence a outro produto")
    void shouldThrowWhenOfferBelongsToAnotherProduct() {
        OfferDTO wrongProductOffer = OfferDTO.builder()
                .id(OFFER_ID).productId(OTHER_PRODUCT_ID).active(true).build();
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(activeProduct());
        when(catalogServiceClient.getOffer(OFFER_ID)).thenReturn(wrongProductOffer);

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Oferta não pertence ao produto informado");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Metrics
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("deve incrementar counter outcome=success após validação bem-sucedida")
    void shouldIncrementSuccessCounter() {
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(activeProduct());
        when(catalogServiceClient.getOffer(OFFER_ID)).thenReturn(activeOffer());

        adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID);

        Counter counter = meterRegistry.find("insurance.catalog.calls.total")
                .tag("outcome", "success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve incrementar counter outcome=validation_error quando IllegalArgumentException é lançada")
    void shouldIncrementValidationErrorCounterOnIllegalArgument() {
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(null); // produto não encontrado

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class);

        Counter counter = meterRegistry.find("insurance.catalog.calls.total")
                .tag("outcome", "validation_error").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("deve registrar timer insurance.catalog.call.duration após chamada bem-sucedida")
    void shouldRecordTimerOnSuccess() {
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(activeProduct());
        when(catalogServiceClient.getOffer(OFFER_ID)).thenReturn(activeOffer());

        adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID);

        Timer timer = meterRegistry.find("insurance.catalog.call.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deve registrar timer insurance.catalog.call.duration mesmo quando validação falha")
    void shouldRecordTimerEvenOnValidationFailure() {
        when(catalogServiceClient.getProduct(PRODUCT_ID)).thenReturn(null);

        assertThatThrownBy(() -> adapter.validateProductAndOffer(PRODUCT_ID, OFFER_ID))
                .isInstanceOf(IllegalArgumentException.class);

        // O timer está no bloco finally — deve ser registrado independente do resultado
        Timer timer = meterRegistry.find("insurance.catalog.call.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Fallback (circuit-breaker open)
    //  Note: @CircuitBreaker is AOP-based and inactive without a Spring context.
    //  The fallback is invoked here directly via reflection to validate its
    //  internal logic (exception type + message + metric counter).
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("catalogFallback deve lançar ServiceUnavailableException com mensagem correta")
    void fallbackShouldThrowServiceUnavailableException() throws Exception {
        RuntimeException cause = new RuntimeException("Circuit breaker triggered");

        Method fallback = CatalogServiceAdapter.class.getDeclaredMethod(
                "catalogFallback", String.class, String.class, Throwable.class);
        fallback.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                fallback.invoke(adapter, PRODUCT_ID, OFFER_ID, cause);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        })
        .isInstanceOf(ServiceUnavailableException.class)
        .hasMessageContaining("Catálogo de produtos temporariamente indisponível");
    }

    @Test
    @DisplayName("catalogFallback deve incrementar counter outcome=circuit_open")
    void fallbackShouldIncrementCircuitOpenCounter() throws Exception {
        RuntimeException cause = new RuntimeException("Circuit breaker triggered");

        Method fallback = CatalogServiceAdapter.class.getDeclaredMethod(
                "catalogFallback", String.class, String.class, Throwable.class);
        fallback.setAccessible(true);

        try {
            fallback.invoke(adapter, PRODUCT_ID, OFFER_ID, cause);
        } catch (InvocationTargetException ite) {
            // expected — we only care about the side-effect (counter)
        }

        Counter counter = meterRegistry.find("insurance.catalog.calls.total")
                .tag("outcome", "circuit_open").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("catalogFallback deve preservar a causa original na ServiceUnavailableException")
    void fallbackShouldPreserveOriginalCause() throws Exception {
        RuntimeException originalCause = new RuntimeException("Timeout connecting to catalog");

        Method fallback = CatalogServiceAdapter.class.getDeclaredMethod(
                "catalogFallback", String.class, String.class, Throwable.class);
        fallback.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                fallback.invoke(adapter, PRODUCT_ID, OFFER_ID, originalCause);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        })
        .isInstanceOf(ServiceUnavailableException.class)
        .hasCause(originalCause);
    }
}

