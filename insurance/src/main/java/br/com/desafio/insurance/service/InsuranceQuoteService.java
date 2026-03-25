package br.com.desafio.insurance.service;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.entity.Customer;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.entity.QuoteStatus;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import br.com.desafio.insurance.domain.validation.QuoteValidator;
import br.com.desafio.insurance.persistence.repository.InsuranceQuoteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implements quote lifecycle use-cases.
 *
 * Validation is fully delegated to {@link QuoteValidator} strategy beans —
 * new rules can be added without touching this class (OCP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceQuoteService implements InsuranceQuoteServicePort {

    /** Depends on the port interface, not the concrete repository class (DIP). */
    private final InsuranceQuoteRepositoryPort quoteRepository;

    /** Ordered list of validators injected by Spring (all @Component QuoteValidator beans). */
    private final List<QuoteValidator> validators;

    @Override
    public InsuranceQuote createAndValidateQuote(InsuranceQuoteRequestDTO request,
                                                 OfferDTO validatedOffer) {
        // Run each validation rule in declaration order
        validators.forEach(v -> v.validate(request, validatedOffer));

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

        InsuranceQuote saved = quoteRepository.save(quote);
        log.debug("Quote created – id: {}", saved.getId());
        return saved;
    }

    @Override
    public Optional<InsuranceQuote> getQuoteById(String quoteId, String documentNumber) {
        return quoteRepository.findById(quoteId, documentNumber);
    }

    @Override
    public InsuranceQuote updateQuoteWithPolicyId(String quoteId,
                                                  String documentNumber,
                                                  Long policyId) {
        InsuranceQuote updated = quoteRepository.updatePolicyId(quoteId, documentNumber, policyId);
        log.debug("Quote {} approved – policyId: {}", quoteId, policyId);
        return updated;
    }
}
