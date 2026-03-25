package br.com.desafio.insurance.adapter.in.http.dto;

import br.com.desafio.insurance.domain.quote.CustomerDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * HTTP response DTO — lives in the adapter layer, not in domain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceQuoteResponseDTO {

    private String id;

    @JsonProperty("insurance_policy_id")
    private Long insurancePolicyId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("offer_id")
    private String offerId;

    private String category;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("total_monthly_premium_amount")
    private BigDecimal totalMonthlyPremiumAmount;

    @JsonProperty("total_coverage_amount")
    private BigDecimal totalCoverageAmount;

    private Map<String, BigDecimal> coverages;

    private List<String> assistances;

    private CustomerDTO customer;
}

