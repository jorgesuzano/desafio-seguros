package br.com.desafio.insurance.adapter.out.catalog;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.ProductDTO;
import br.com.desafio.insurance.domain.port.out.CatalogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter: implements CatalogPort by delegating to the external catalog service
 * via Feign. Renamed from CatalogValidationService — this class is an adapter, not an
 * application service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogServiceAdapter implements CatalogPort {

    private final CatalogServiceClient catalogServiceClient;

    @Override
    public OfferDTO validateProductAndOffer(String productId, String offerId) {
        ProductDTO product = catalogServiceClient.getProduct(productId);
        validateProduct(productId, product);

        OfferDTO offer = catalogServiceClient.getOffer(offerId);
        validateOffer(offerId, productId, offer);

        log.debug("Product {} and offer {} validated", productId, offerId);
        return offer;
    }

    private void validateProduct(String productId, ProductDTO product) {
        if (product == null)
            throw new IllegalArgumentException("Produto não encontrado: " + productId);
        if (!Boolean.TRUE.equals(product.getActive()))
            throw new IllegalArgumentException("Produto inativo: " + productId);
    }

    private void validateOffer(String offerId, String productId, OfferDTO offer) {
        if (offer == null)
            throw new IllegalArgumentException("Oferta não encontrada: " + offerId);
        if (!Boolean.TRUE.equals(offer.getActive()))
            throw new IllegalArgumentException("Oferta inativa: " + offerId);
        if (!productId.equals(offer.getProductId()))
            throw new IllegalArgumentException("Oferta não pertence ao produto informado");
    }
}

