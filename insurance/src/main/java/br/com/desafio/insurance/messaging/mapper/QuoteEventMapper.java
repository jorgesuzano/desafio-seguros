package br.com.desafio.insurance.messaging.mapper;

import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class QuoteEventMapper {

    public InsuranceQuoteReceivedEvent toEvent(InsuranceQuote quote,
                                               InsuranceQuoteRequestDTO request) {
        return InsuranceQuoteReceivedEvent.builder()
                .quoteId(quote.getId())
                .productId(request.getProductId())
                .offerId(request.getOfferId())
                .category(request.getCategory())
                .totalMonthlyPremiumAmount(request.getTotalMonthlyPremiumAmount())
                .totalCoverageAmount(request.getTotalCoverageAmount())
                .coverages(request.getCoverages())
                .assistances(request.getAssistances())
                .customer(toCustomerMap(request))
                .receivedAt(LocalDateTime.now().toString())
                .build();
    }

    private Map<String, Object> toCustomerMap(InsuranceQuoteRequestDTO request) {
        var c = request.getCustomer();
        Map<String, Object> map = new HashMap<>();
        map.put("document_number", c.getDocumentNumber());
        map.put("name",            c.getName());
        map.put("type",            c.getType().toString());
        map.put("gender",          c.getGender().toString());
        map.put("date_of_birth",   c.getDateOfBirth());
        map.put("email",           c.getEmail());
        map.put("phone_number",    c.getPhoneNumber());
        return map;
    }
}

