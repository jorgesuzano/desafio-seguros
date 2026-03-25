package br.com.desafio.insurance.domain.validation;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;

@FunctionalInterface
public interface QuoteValidator {
    void validate(InsuranceQuoteRequestDTO request, OfferDTO offer);
}

