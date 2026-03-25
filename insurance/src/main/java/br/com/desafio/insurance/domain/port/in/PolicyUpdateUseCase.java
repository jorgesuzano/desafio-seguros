package br.com.desafio.insurance.domain.port.in;

import br.com.desafio.insurance.domain.model.InsuranceQuote;

/**
 * Inbound port: use case driven by the messaging adapter (SQS consumer).
 * ISP: isolated from the HTTP use cases to avoid forcing the listener
 * to depend on methods it does not use.
 */
public interface PolicyUpdateUseCase {

    InsuranceQuote updateQuoteWithPolicyId(String quoteId, String documentNumber, Long policyId);
}

