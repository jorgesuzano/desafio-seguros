package br.com.desafio.insurance.http.controller;


import br.com.desafio.insurance.domain.InsuranceQuoteRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/quotes")
public class InsuranceQuoteController {

    @PostMapping
    public ResponseEntity<?> createQuote(@Valid @RequestBody InsuranceQuoteRequestDTO request) {
        // Lógica de criação da cotação
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}