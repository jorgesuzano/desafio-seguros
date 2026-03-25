package br.com.desafio.insurance.domain.port.in;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;

import java.util.Optional;

/**
 * Inbound port: use cases driven by the HTTP adapter (controller).
 */
public interface InsuranceQuoteUseCase {

    InsuranceQuote createAndValidateQuote(InsuranceQuoteRequestDTO request, OfferDTO validatedOffer);

    Optional<InsuranceQuote> getQuoteById(String quoteId, String documentNumber);
}

