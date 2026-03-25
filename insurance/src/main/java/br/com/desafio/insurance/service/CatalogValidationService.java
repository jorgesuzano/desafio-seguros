package br.com.desafio.insurance.service;


import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.ProductDTO;
import br.com.desafio.insurance.http.client.CatalogServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogValidationService implements CatalogPort {

    private final CatalogServiceClient catalogServiceClient;

    /**
     * Valida produto e oferta juntos
     *
     * @param productId ID do produto
     * @param offerId ID da oferta
     * @return OfferDTO com dados da oferta validada
     */
    @Override
    public OfferDTO validateProductAndOffer(String productId, String offerId) {
        ProductDTO product = catalogServiceClient.getProduct(productId);
        validateProduct(productId, product);

        OfferDTO offer = catalogServiceClient.getOffer(offerId);
        validateOffer(offerId, productId, offer);

        log.debug("Produto {} e oferta {} validados", productId, offerId);
        return offer;
    }

    // ---- métodos auxiliares privados (detalhes da implementação, não fazem parte da interface) ----

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