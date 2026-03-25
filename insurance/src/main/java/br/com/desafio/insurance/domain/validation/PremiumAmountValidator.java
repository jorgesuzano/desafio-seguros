package br.com.desafio.insurance.domain.validation;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.PremiumAmountDTO;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(3)
public class PremiumAmountValidator implements QuoteValidator {

    @Override
    public void validate(InsuranceQuoteRequestDTO request, OfferDTO offer) {
        BigDecimal premium = request.getTotalMonthlyPremiumAmount();
        PremiumAmountDTO range = offer.getMonthlyPremiumAmount();

        if (premium.compareTo(range.getMinAmount()) < 0)
            throw new IllegalArgumentException(
                    "Valor do prêmio mensal (" + premium +
                    ") está abaixo do mínimo permitido (" + range.getMinAmount() + ")");

        if (premium.compareTo(range.getMaxAmount()) > 0)
            throw new IllegalArgumentException(
                    "Valor do prêmio mensal (" + premium +
                    ") excede o máximo permitido (" + range.getMaxAmount() + ")");
    }

}
