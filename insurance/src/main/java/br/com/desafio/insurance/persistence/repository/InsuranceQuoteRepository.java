package br.com.desafio.insurance.persistence.repository;

import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.entity.QuoteStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InsuranceQuoteRepository implements InsuranceQuoteRepositoryPort {

    private final DynamoDbTable<InsuranceQuote> insuranceQuoteTable;

    @Override
    public InsuranceQuote save(InsuranceQuote quote) {
        log.debug("Saving insurance quote – id: {}", quote.getId());
        quote.preWrite();
        insuranceQuoteTable.putItem(quote);
        return quote;
    }

    @Override
    public Optional<InsuranceQuote> findById(String id, String documentNumber) {
        log.debug("Finding quote – id: {} documentNumber: {}", id, documentNumber);
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(documentNumber)
                .build();
        return Optional.ofNullable(insuranceQuoteTable.getItem(key));
    }

    @Override
    public InsuranceQuote updatePolicyId(String quoteId, String documentNumber, Long policyId) {
        InsuranceQuote quote = findById(quoteId, documentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + quoteId));
        quote.setInsurancePolicyId(policyId);
        quote.setStatus(QuoteStatus.APPROVED);
        return save(quote);
    }

    public List<InsuranceQuote> findByDocumentNumber(String documentNumber) {
        log.debug("Finding quotes by documentNumber: {}", documentNumber);
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional
                        .sortBeginsWith(qc -> qc.partitionValue(documentNumber).sortValue(documentNumber)))
                .build();
        return insuranceQuoteTable.query(request)
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    public void delete(InsuranceQuote quote) {
        log.debug("Deleting quote – id: {}", quote.getId());
        Key key = Key.builder()
                .partitionValue(quote.getId())
                .sortValue(quote.getDocumentNumber())
                .build();
        insuranceQuoteTable.deleteItem(key);
    }

    public List<InsuranceQuote> findAll() {
        log.debug("Finding all quotes");
        return insuranceQuoteTable.scan()
                .items()
                .stream()
                .collect(Collectors.toList());
    }
}
