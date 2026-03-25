package br.com.desafio.insurance.domain.validation;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(4)
public class TotalCoverageAmountValidator implements QuoteValidator {

    @Override
    public void validate(InsuranceQuoteRequestDTO request, OfferDTO offer) {
        BigDecimal sum = request.getCoverages().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(request.getTotalCoverageAmount()) != 0)
            throw new IllegalArgumentException(
                    "Valor total das coberturas (" + sum +
                    ") não corresponde ao valor informado (" + request.getTotalCoverageAmount() + ")");
    }

}
