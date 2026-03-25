package br.com.desafio.insurance.persistence.repository;

import br.com.desafio.insurance.domain.entity.InsuranceQuote;

import java.util.Optional;

/**
 * Porta de persistência para cotações.
 * Mantém o serviço de domínio desacoplado do mecanismo de armazenamento (DIP).
 */
public interface InsuranceQuoteRepositoryPort {

    InsuranceQuote save(InsuranceQuote quote);

    Optional<InsuranceQuote> findById(String id, String documentNumber);

    InsuranceQuote updatePolicyId(String quoteId, String documentNumber, Long policyId);
}

