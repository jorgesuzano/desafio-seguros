package br.com.desafio.insurance.domain.quote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceQuoteRequestDTO {

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("offer_id")
    private String offerId;

    @JsonProperty("category")
    private String category;

    @JsonProperty("total_monthly_premium_amount")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total do prêmio mensal deve ser maior que 0")
    private BigDecimal totalMonthlyPremiumAmount;

    @JsonProperty("total_coverage_amount")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total de coberturas deve ser maior que 0")
    private BigDecimal totalCoverageAmount;

    @JsonProperty("coverages")
    private Map<String, BigDecimal> coverages;

    @JsonProperty("assistances")
    private List<String> assistances;

    @JsonProperty("customer")
    private CustomerDTO customer;
}