package br.com.desafio.insurance.adapter.out.persistence;

import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.model.QuoteStatus;
import br.com.desafio.insurance.domain.port.out.InsuranceQuoteRepositoryPort;
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
        quote.preWrite();
        insuranceQuoteTable.putItem(quote);
        log.debug("Quote saved – id: {}", quote.getId());
        return quote;
    }

    @Override
    public Optional<InsuranceQuote> findById(String id, String documentNumber) {
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
        Key key = Key.builder()
                .partitionValue(quote.getId())
                .sortValue(quote.getDocumentNumber())
                .build();
        insuranceQuoteTable.deleteItem(key);
        log.debug("Quote deleted – id: {}", quote.getId());
    }

    public List<InsuranceQuote> findAll() {
        return insuranceQuoteTable.scan()
                .items()
                .stream()
                .collect(Collectors.toList());
    }
}

