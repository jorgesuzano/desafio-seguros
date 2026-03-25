package br.com.desafio.insurance.http.controller;

import br.com.desafio.insurance.domain.dto.InsuranceQuoteResponseDTO;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.quote.CustomerDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

