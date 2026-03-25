package br.com.desafio.insurance.mock;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.PremiumAmountDTO;
import br.com.desafio.insurance.domain.catalog.ProductDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock Server para simular a API de Catálogo
 * Use este controller apenas em desenvolvimento/testes
 */
@RestController
@RequestMapping("/mock/catalog")
public class CatalogMockController {

    private static final String PRODUCT_ID = "1b2da7cc-b367-4196-8a78-9cfeec21f587";
    private static final String OFFER_ID = "adc56d77-348c-4bf0-908f-22d402ee715c";

    /**
     * Mock: Retorna dados de um produto
     */
    @GetMapping("/products/{id}")
    public ProductDTO getProduct(@PathVariable String id) {
        if (PRODUCT_ID.equals(id)) {
            return ProductDTO.builder()
                .id(id)
                .name("Seguro de Vida")
                .createdAt("2021-07-01T00:00:00")
                .active(true)
                .offers(Arrays.asList(
                    "adc56d77-348c-4bf0-908f-22d402ee715c",
                    "bdc56d77-348c-4bf0-908f-22d402ee715c",
                    "cdc56d77-348c-4bf0-908f-22d402ee715c"
                ))
                .build();
        }
        throw new IllegalArgumentException("Produto não encontrado: " + id);
    }

    /**
     * Mock: Retorna dados de uma oferta
     */
    @GetMapping("/offers/{id}")
    public OfferDTO getOffer(@PathVariable String id) {
        if (OFFER_ID.equals(id)) {
            Map<String, BigDecimal> coverages = new HashMap<>();
            coverages.put("Incêndio", new BigDecimal("500000.00"));
            coverages.put("Desastres naturais", new BigDecimal("600000.00"));
            coverages.put("Responsabiliadade civil", new BigDecimal("80000.00"));
            coverages.put("Roubo", new BigDecimal("100000.00"));

            return OfferDTO.builder()
                .id(id)
                .productId(PRODUCT_ID)
                .name("Seguro de Vida Familiar")
                .createdAt("2021-07-01T00:00:00")
                .active(true)
                .coverages(coverages)
                .assistances(Arrays.asList(
                    "Encanador",
                    "Eletricista",
                    "Chaveiro 24h",
                    "Assistência Funerária"
                ))
                .monthlyPremiumAmount(PremiumAmountDTO.builder()
                    .maxAmount(new BigDecimal("100.74"))
                    .minAmount(new BigDecimal("50.00"))
                    .suggestedAmount(new BigDecimal("60.25"))
                    .build())
                .build();
        }
        throw new IllegalArgumentException("Oferta não encontrada: " + id);
    }
}