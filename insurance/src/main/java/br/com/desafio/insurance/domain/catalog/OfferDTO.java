package br.com.desafio.insurance.domain.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para dados de Oferta do Catálogo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDTO {

    private String id;
    private String productId;
    private String name;
    private String createdAt;
    private Boolean active;
    private Map<String, BigDecimal> coverages;
    private List<String> assistances;
    private PremiumAmountDTO monthlyPremiumAmount;
}