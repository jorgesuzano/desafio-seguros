package br.com.desafio.insurance.domain.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumAmountDTO {

    private BigDecimal maxAmount;
    private BigDecimal minAmount;
    private BigDecimal suggestedAmount;
}