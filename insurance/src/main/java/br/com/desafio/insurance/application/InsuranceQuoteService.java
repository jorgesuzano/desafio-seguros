package br.com.desafio.insurance.application;

import br.com.desafio.insurance.domain.catalog.OfferDTO;
import br.com.desafio.insurance.domain.model.Customer;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.model.QuoteStatus;
import br.com.desafio.insurance.domain.port.in.InsuranceQuoteUseCase;
import br.com.desafio.insurance.domain.port.in.PolicyUpdateUseCase;
import br.com.desafio.insurance.domain.port.out.InsuranceQuoteRepositoryPort;
import br.com.desafio.insurance.domain.quote.InsuranceQuoteRequestDTO;
import br.com.desafio.insurance.domain.validation.QuoteValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceQuoteService implements InsuranceQuoteUseCase, PolicyUpdateUseCase {

    private final InsuranceQuoteRepositoryPort quoteRepository;
    private final List<QuoteValidator> validators;

    @Override
    public InsuranceQuote createAndValidateQuote(InsuranceQuoteRequestDTO request,
                                                 OfferDTO validatedOffer) {
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

