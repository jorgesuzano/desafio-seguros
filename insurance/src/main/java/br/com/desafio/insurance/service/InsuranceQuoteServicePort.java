package br.com.desafio.insurance.service;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.entity.QuoteStatus;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;

import java.util.Optional;

/**
 * Port (interface) for insurance quote use-cases.
 * Follows DIP: high-level modules depend on this abstraction, not on the concrete class.
 */
public interface InsuranceQuoteServicePort {

    InsuranceQuote createAndValidateQuote(InsuranceQuoteRequestDTO request, OfferDTO validatedOffer);

    Optional<InsuranceQuote> getQuoteById(String quoteId, String documentNumber);

    InsuranceQuote updateQuoteWithPolicyId(String quoteId, String documentNumber, Long policyId);
}

