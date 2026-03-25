package br.com.desafio.insurance.persistence.repository;

import br.com.desafio.insurance.domain.entity.InsuranceQuote;

import java.util.Optional;

public interface InsuranceQuoteRepositoryPort {

    InsuranceQuote save(InsuranceQuote quote);

    Optional<InsuranceQuote> findById(String id, String documentNumber);

    InsuranceQuote updatePolicyId(String quoteId, String documentNumber, Long policyId);
}

