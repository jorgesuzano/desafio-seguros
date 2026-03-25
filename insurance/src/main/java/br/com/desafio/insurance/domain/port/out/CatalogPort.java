package br.com.desafio.insurance.domain.port.out;

import br.com.desafio.insurance.domain.catalog.OfferDTO;

/**
 * Outbound port: contract that the catalog adapter must fulfill.
 * DIP: application layer depends on this abstraction, not on Feign or HTTP details.
 */
public interface CatalogPort {
    OfferDTO validateProductAndOffer(String productId, String offerId);
}

