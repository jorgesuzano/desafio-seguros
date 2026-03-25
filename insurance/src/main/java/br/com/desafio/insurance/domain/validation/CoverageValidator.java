package br.com.desafio.insurance.domain.validation;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Order(1)
public class CoverageValidator implements QuoteValidator {

    @Override
    public void validate(InsuranceQuoteRequestDTO request, OfferDTO offer) {
        Map<String, BigDecimal> requested = request.getCoverages();
        Map<String, BigDecimal> allowed   = offer.getCoverages();

        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException("Pelo menos uma cobertura é obrigatória");
        }

        requested.forEach((name, value) -> {
            if (!allowed.containsKey(name)) {
                throw new IllegalArgumentException(
                        "Cobertura '" + name + "' não está disponível para esta oferta");
            }
            if (value.compareTo(allowed.get(name)) > 0) {
                throw new IllegalArgumentException(
                        "Valor da cobertura '" + name + "' (" + value +
                        ") excede o máximo permitido (" + allowed.get(name) + ")");
            }
        });
    }
}

