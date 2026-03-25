package br.com.desafio.insurance.service;
import br.com.desafio.insurance.domain.catalog.OfferDTO;

public interface CatalogPort {
    OfferDTO validateProductAndOffer(String productId, String offerId);
}
