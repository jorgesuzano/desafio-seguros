package br.com.desafio.insurance.adapter.in.http.mapper;

import br.com.desafio.insurance.adapter.in.http.dto.InsuranceQuoteResponseDTO;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.quote.CustomerDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper: converts the domain InsuranceQuote model to the HTTP response DTO.
 * SRP: mapper responsibility is isolated from the controller.
 */
@Component
public class InsuranceQuoteResponseMapper {

    public InsuranceQuoteResponseDTO toDTO(InsuranceQuote q) {
        return InsuranceQuoteResponseDTO.builder()
                .id(q.getId())
                .insurancePolicyId(q.getInsurancePolicyId())
                .productId(q.getProductId())
                .offerId(q.getOfferId())
                .category(q.getCategory())
                .createdAt(toLocalDateTime(q.getCreatedAt()))
                .updatedAt(toLocalDateTime(q.getUpdatedAt()))
                .totalMonthlyPremiumAmount(q.getTotalMonthlyPremiumAmount())
                .totalCoverageAmount(q.getTotalCoverageAmount())
                .coverages(q.getCoverages())
                .assistances(q.getAssistances())
                .customer(CustomerDTO.builder()
                        .documentNumber(q.getCustomer().getDocumentNumber())
                        .name(q.getCustomer().getName())
                        .type(q.getCustomer().getType())
                        .gender(q.getCustomer().getGender())
                        .dateOfBirth(q.getCustomer().getDateOfBirth())
                        .email(q.getCustomer().getEmail())
                        .phoneNumber(q.getCustomer().getPhoneNumber())
                        .build())
                .build();
    }

    private LocalDateTime toLocalDateTime(Long epochMs) {
        return epochMs == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
    }
}

