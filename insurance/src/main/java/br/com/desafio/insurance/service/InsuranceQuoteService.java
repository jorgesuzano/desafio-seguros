package br.com.desafio.insurance.service;

import br.com.desafio.insurance.domain.entity.Customer;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.entity.QuoteStatus;
import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.catalog.PremiumAmountDTO;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import br.com.desafio.insurance.persistence.repository.InsuranceQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceQuoteService {

    private final InsuranceQuoteRepository quoteRepository;

    public InsuranceQuote createAndValidateQuote(
        InsuranceQuoteRequestDTO request,
        OfferDTO validatedOffer) throws IllegalArgumentException {

        log.info("Validating and creating new insurance quote for product: {} and offer: {}",
            request.getProductId(), request.getOfferId());

        validateCoverages(request, validatedOffer);
        validateAssistances(request, validatedOffer);
        validateMonthlyPremium(request, validatedOffer);
        validateTotalCoverageAmount(request);

        Customer customer = Customer.builder()
            .documentNumber(request.getCustomer().getDocumentNumber())
            .name(request.getCustomer().getName())
            .type(request.getCustomer().getType())
            .gender(request.getCustomer().getGender())
            .dateOfBirth(request.getCustomer().getDateOfBirth())
            .email(request.getCustomer().getEmail())
            .phoneNumber(request.getCustomer().getPhoneNumber())
            .build();

        InsuranceQuote quote = InsuranceQuote.builder()
            .productId(request.getProductId())
            .offerId(request.getOfferId())
            .category(request.getCategory())
            .totalMonthlyPremiumAmount(request.getTotalMonthlyPremiumAmount())
            .totalCoverageAmount(request.getTotalCoverageAmount())
            .coverages(request.getCoverages())
            .assistances(request.getAssistances())
            .customer(customer)
            .documentNumber(request.getCustomer().getDocumentNumber())
            .customerName(request.getCustomer().getName())
            .status(QuoteStatus.RECEIVED)
            .build();

        InsuranceQuote savedQuote = quoteRepository.save(quote);
        log.info("Quote created and validated successfully with ID: {}", savedQuote.getId());

        return savedQuote;
    }

    private void validateCoverages(InsuranceQuoteRequestDTO request, OfferDTO offer)
        throws IllegalArgumentException {

        log.debug("Validating coverages for quote");

        Map<String, BigDecimal> requestCoverages = request.getCoverages();
        Map<String, BigDecimal> offerCoverages = offer.getCoverages();

        if (requestCoverages == null || requestCoverages.isEmpty()) {
            throw new IllegalArgumentException("Pelo menos uma cobertura é obrigatória");
        }

        for (String coverage : requestCoverages.keySet()) {
            if (!offerCoverages.containsKey(coverage)) {
                throw new IllegalArgumentException(
                    "Cobertura '" + coverage + "' não está disponível para esta oferta"
                );
            }

            BigDecimal requestAmount = requestCoverages.get(coverage);
            BigDecimal maxAmount = offerCoverages.get(coverage);

            if (requestAmount.compareTo(maxAmount) > 0) {
                throw new IllegalArgumentException(
                    "Valor da cobertura '" + coverage + "' (" + requestAmount +
                    ") excede o máximo permitido (" + maxAmount + ")"
                );
            }
        }

        log.debug("Coverages validated successfully");
    }

    private void validateAssistances(InsuranceQuoteRequestDTO request, OfferDTO offer)
        throws IllegalArgumentException {

        log.debug("Validating assistances for quote");

        List<String> requestAssistances = request.getAssistances();
        List<String> offerAssistances = offer.getAssistances();

        if (requestAssistances == null || requestAssistances.isEmpty()) {
            throw new IllegalArgumentException("Pelo menos uma assistência é obrigatória");
        }

        for (String assistance : requestAssistances) {
            if (!offerAssistances.contains(assistance)) {
                throw new IllegalArgumentException(
                    "Assistência '" + assistance + "' não está disponível para esta oferta"
                );
            }
        }

        log.debug("Assistances validated successfully");
    }

    private void validateMonthlyPremium(InsuranceQuoteRequestDTO request, OfferDTO offer)
        throws IllegalArgumentException {

        log.debug("Validating monthly premium for quote");

        BigDecimal requestPremium = request.getTotalMonthlyPremiumAmount();
        PremiumAmountDTO premiumRange = offer.getMonthlyPremiumAmount();

        if (requestPremium.compareTo(premiumRange.getMinAmount()) < 0) {
            throw new IllegalArgumentException(
                "Valor do prêmio mensal (" + requestPremium +
                ") está abaixo do mínimo permitido (" + premiumRange.getMinAmount() + ")"
            );
        }

        if (requestPremium.compareTo(premiumRange.getMaxAmount()) > 0) {
            throw new IllegalArgumentException(
                "Valor do prêmio mensal (" + requestPremium +
                ") excede o máximo permitido (" + premiumRange.getMaxAmount() + ")"
            );
        }

        log.debug("Monthly premium validated successfully");
    }

    private void validateTotalCoverageAmount(InsuranceQuoteRequestDTO request)
        throws IllegalArgumentException {

        log.debug("Validating total coverage amount for quote");

        Map<String, BigDecimal> coverages = request.getCoverages();
        BigDecimal calculatedTotal = coverages.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal requestedTotal = request.getTotalCoverageAmount();

        if (calculatedTotal.compareTo(requestedTotal) != 0) {
            throw new IllegalArgumentException(
                "Valor total das coberturas (" + calculatedTotal +
                ") não corresponde ao valor informado (" + requestedTotal + ")"
            );
        }

        log.debug("Total coverage amount validated successfully");
    }

    public Optional<InsuranceQuote> getQuoteById(String quoteId, String documentNumber) {
        return quoteRepository.findById(quoteId, documentNumber);
    }

//    public List<InsuranceQuote> getQuotesByStatus(QuoteStatus status) {
//        return quoteRepository.findByStatus(status);
//    }

    public List<InsuranceQuote> getQuotesByCustomer(String documentNumber) {
        return quoteRepository.findByDocumentNumber(documentNumber);
    }

    public InsuranceQuote updateQuoteStatus(String quoteId, String documentNumber, QuoteStatus newStatus) {
        InsuranceQuote quote = quoteRepository.findById(quoteId, documentNumber)
            .orElseThrow(() -> new IllegalArgumentException("Quote not found"));

        quote.setStatus(newStatus);
        return quoteRepository.save(quote);
    }

    public InsuranceQuote updateQuoteWithPolicyId(Long quoteId, Long policyId) {
        log.warn("updateQuoteWithPolicyId called with Long ID - needs refactoring");
        throw new RuntimeException("Use the overloaded method with documentNumber parameter");
    }

    public InsuranceQuote updateQuoteWithPolicyId(String quoteId, String documentNumber, Long policyId) {
        InsuranceQuote quote = quoteRepository.findById(quoteId, documentNumber)
            .orElseThrow(() -> new IllegalArgumentException("Quote not found"));

        quote.setInsurancePolicyId(policyId);
        quote.setStatus(QuoteStatus.APPROVED);
        log.info("Quote {} updated with insurance policy ID: {}", quoteId, policyId);
        return quoteRepository.save(quote);
    }
}

