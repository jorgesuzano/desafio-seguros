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

/**
 * DTO para requisição de cotação de seguro
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceQuoteRequestDTO {

    @JsonProperty("product_id")
    @NotBlank(message = "ID do produto é obrigatório")
    private String productId;

    @JsonProperty("offer_id")
    @NotBlank(message = "ID da oferta é obrigatório")
    private String offerId;

    @JsonProperty("category")
    @NotNull(message = "Categoria é obrigatória")
    private String category;

    @JsonProperty("total_monthly_premium_amount")
    @NotNull(message = "Total do prêmio mensal é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total do prêmio mensal deve ser maior que 0")
    private BigDecimal totalMonthlyPremiumAmount;

    @JsonProperty("total_coverage_amount")
    @NotNull(message = "Total de coberturas é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total de coberturas deve ser maior que 0")
    private BigDecimal totalCoverageAmount;

    @JsonProperty("coverages")
    @NotEmpty(message = "Pelo menos uma cobertura é obrigatória")
    private Map<String, BigDecimal> coverages;

    @JsonProperty("assistances")
    @NotEmpty(message = "Pelo menos uma assistência é obrigatória")
    private List<String> assistances;

    @JsonProperty("customer")
    @Valid
    @NotNull(message = "Dados do cliente são obrigatórios")
    private CustomerDTO customer;
}