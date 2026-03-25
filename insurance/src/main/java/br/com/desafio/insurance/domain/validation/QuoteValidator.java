package br.com.desafio.insurance.domain.validation;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;

/**
 * Strategy interface for quote validation rules.
 * Each implementation encapsulates a single business rule (SRP + OCP).
 * Adding a new validation requires only a new @Component, no modification to existing code.
 */
@FunctionalInterface
public interface QuoteValidator {
    void validate(InsuranceQuoteRequestDTO request, OfferDTO offer);
}

