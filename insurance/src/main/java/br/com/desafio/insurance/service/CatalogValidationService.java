package br.com.desafio.insurance.service;


import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.ProductDTO;
import br.com.desafio.insurance.http.client.CatalogServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serviço para validar produto e oferta contra o Catálogo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogValidationService {

    private final CatalogServiceClient catalogServiceClient;

    /**
     * Valida se o produto existe e está ativo
     *
     * @param productId ID do produto a validar
     * @return true se produto existe e está ativo
     * @throws IllegalArgumentException se produto não encontrado ou inativo
     */
    public boolean validateProduct(String productId) {
        try {
            ProductDTO product = catalogServiceClient.getProduct(productId);

            if (product == null) {
                log.warn("Produto não encontrado: {}", productId);
                throw new IllegalArgumentException("Produto não encontrado: " + productId);
            }

            if (!Boolean.TRUE.equals(product.getActive())) {
                log.warn("Produto inativo: {}", productId);
                throw new IllegalArgumentException("Produto inativo: " + productId);
            }

            log.info("Produto validado com sucesso: {}", productId);
            return true;

        } catch (Exception e) {
            log.error("Erro ao validar produto: {}", productId, e);
            throw new IllegalArgumentException("Erro ao validar produto: " + e.getMessage());
            // ⚠️ Mascara exceção real (FeignException, timeout, etc)
        }
    }

    /**
     * Valida se a oferta existe, está ativa e pertence ao produto informado
     *
     * @param offerId ID da oferta a validar
     * @param productId ID do produto esperado
     * @return true se oferta é válida
     * @throws IllegalArgumentException se oferta não encontrada, inativa ou não pertence ao produto
     */
    public boolean validateOffer(String offerId, String productId) {
        try {
            OfferDTO offer = catalogServiceClient.getOffer(offerId);

            if (offer == null) {
                log.warn("Oferta não encontrada: {}", offerId);
                throw new IllegalArgumentException("Oferta não encontrada: " + offerId);
            }

            if (!Boolean.TRUE.equals(offer.getActive())) {
                log.warn("Oferta inativa: {}", offerId);
                throw new IllegalArgumentException("Oferta inativa: " + offerId);
            }

            if (!offer.getProductId().equals(productId)) {
                log.warn("Oferta {} não pertence ao produto {}", offerId, productId);
                throw new IllegalArgumentException(
                    "Oferta não pertence ao produto informado"
                );
            }

            log.info("Oferta validada com sucesso: {}", offerId);
            return true;

        } catch (Exception e) {
            log.error("Erro ao validar oferta: {}", offerId, e);
            throw new IllegalArgumentException("Erro ao validar oferta: " + e.getMessage());
        }
    }

    /**
     * Valida produto e oferta juntos
     *
     * @param productId ID do produto
     * @param offerId ID da oferta
     * @return OfferDTO com dados da oferta validada
     */
    public OfferDTO validateProductAndOffer(String productId, String offerId) {
        validateProduct(productId);
        validateOffer(offerId, productId);
        return catalogServiceClient.getOffer(offerId);
    }
}