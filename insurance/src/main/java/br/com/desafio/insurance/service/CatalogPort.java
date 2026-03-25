package br.com.desafio.insurance.service;
import br.com.desafio.insurance.domain.catalog.OfferDTO;
/** Port for catalog validation (DIP + ISP). */
public interface CatalogPort {
    OfferDTO validateProductAndOffer(String productId, String offerId);
}
