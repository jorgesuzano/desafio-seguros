package br.com.desafio.insurance.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.*;
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

    @NotBlank(message = "ID do produto é obrigatório")
    private String productId;

    @NotBlank(message = "ID da oferta é obrigatório")
    private String offerId;

    @NotNull(message = "Categoria é obrigatória")
    private String category;

    @NotNull(message = "Total do prêmio mensal é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total do prêmio mensal deve ser maior que 0")
    private BigDecimal totalMonthlyPremiumAmount;

    @NotNull(message = "Total de coberturas é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total de coberturas deve ser maior que 0")
    private BigDecimal totalCoverageAmount;

    @NotEmpty(message = "Pelo menos uma cobertura é obrigatória")
    private Map<String, BigDecimal> coverages;

    @NotEmpty(message = "Pelo menos uma assistência é obrigatória")
    private List<String> assistances;

    @Valid
    @NotNull(message = "Dados do cliente são obrigatórios")
    private CustomerDTO customer;
}