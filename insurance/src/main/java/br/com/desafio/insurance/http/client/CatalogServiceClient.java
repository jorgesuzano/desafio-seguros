package br.com.desafio.insurance.http.client;


import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.ProductDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign para consumir a API de Catálogo
 */
@FeignClient(name = "catalog-service", url = "http://localhost:8080/mock/catalog")
public interface CatalogServiceClient {

    @GetMapping("/products/{id}")
    @Cacheable(value = "products", key = "#id")
    ProductDTO getProduct(@PathVariable String id);

    @GetMapping("/offers/{id}")
    @Cacheable(value = "offers", key = "#id")
    OfferDTO getOffer(@PathVariable String id);
}