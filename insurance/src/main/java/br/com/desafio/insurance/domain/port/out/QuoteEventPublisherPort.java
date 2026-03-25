package br.com.desafio.insurance.domain.port.out;

import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;

/**
 * Outbound port: contract for publishing quote events.
 * DIP: the HTTP adapter depends on this abstraction, not on SQS/infrastructure details.
 */
public interface QuoteEventPublisherPort {
    void publish(InsuranceQuoteReceivedEvent event);
}

