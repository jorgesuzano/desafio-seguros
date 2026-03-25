package br.com.desafio.insurance.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePolicyCreatedEvent {
    
    @JsonProperty("policy_id")
    private Long policyId;
    
    @JsonProperty("quote_id")
    private String quoteId;
    
    @JsonProperty("document_number")
    private String documentNumber;
    
    @JsonProperty("issued_at")
    private LocalDateTime issuedAt;
}

