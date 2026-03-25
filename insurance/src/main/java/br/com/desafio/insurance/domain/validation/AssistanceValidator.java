package br.com.desafio.insurance.domain.validation;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
public class AssistanceValidator implements QuoteValidator {

    @Override
    public void validate(InsuranceQuoteRequestDTO request, OfferDTO offer) {
        List<String> requested = request.getAssistances();

        if (requested == null || requested.isEmpty())
            throw new IllegalArgumentException("Pelo menos uma assistência é obrigatória");

        requested.stream()
                .filter(a -> !offer.getAssistances().contains(a))
                .findFirst()
                .ifPresent(a -> {
                    throw new IllegalArgumentException(
                            "Assistência '" + a + "' não está disponível para esta oferta");
                });
    }
}
