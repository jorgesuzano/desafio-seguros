package br.com.desafio.insurance.http.controller;


import br.com.desafio.insurance.service.CatalogValidationService;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import br.com.desafio.insurance.domain.catalog.OfferDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class InsuranceQuoteController {

//    private final CatalogValidationService catalogValidationService;

    @PostMapping
    public ResponseEntity<?> createQuote(@RequestBody InsuranceQuoteRequestDTO request) {
        try {
            log.info("Recebida requisição de cotação para produto: {} e oferta: {}",
                    request.getProductId(), request.getOfferId());

            // Valida produto e oferta contra o catálogo
//            OfferDTO validatedOffer = catalogValidationService
//                    .validateProductAndOffer(request.getProductId(), request.getOfferId());

//            log.info("Cotação validada com sucesso. Oferta: {}", validatedOffer.getName());


            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Cotação criada com sucesso");

        } catch (IllegalArgumentException e) {
            log.error("Erro de validação na cotação: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());

        } catch (Exception e) {
            log.error("Erro ao processar cotação", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao processar cotação");
        }
    }
}