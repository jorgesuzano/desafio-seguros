package br.com.desafio.insurance.domain.port.out;

import br.com.desafio.insurance.domain.model.InsuranceQuote;

import java.util.Optional;

/**
 * Outbound port: contract that the persistence adapter must fulfill.
 * DIP: application layer depends on this abstraction, not on DynamoDB details.
 */
public interface InsuranceQuoteRepositoryPort {

    InsuranceQuote save(InsuranceQuote quote);

    Optional<InsuranceQuote> findById(String id, String documentNumber);

    InsuranceQuote updatePolicyId(String quoteId, String documentNumber, Long policyId);
}

