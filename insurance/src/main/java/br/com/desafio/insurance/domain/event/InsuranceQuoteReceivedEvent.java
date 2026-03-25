package br.com.desafio.insurance.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceQuoteReceivedEvent {
    
    @JsonProperty("quote_id")
    private String quoteId;
    
    @JsonProperty("product_id")
    private String productId;
    
    @JsonProperty("offer_id")
    private String offerId;
    
    private String category;
    
    @JsonProperty("total_monthly_premium_amount")
    private BigDecimal totalMonthlyPremiumAmount;
    
    @JsonProperty("total_coverage_amount")
    private BigDecimal totalCoverageAmount;
    
    private Map<String, BigDecimal> coverages;
    
    private List<String> assistances;
    
    @JsonProperty("customer")
    private Map<String, Object> customer;
    
    @JsonProperty("received_at")
    private LocalDateTime receivedAt;
}

