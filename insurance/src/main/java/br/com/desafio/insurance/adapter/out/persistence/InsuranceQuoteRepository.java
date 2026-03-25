package br.com.desafio.insurance.adapter.out.persistence;

import br.com.desafio.insurance.domain.exception.ServiceUnavailableException;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.model.QuoteStatus;
import br.com.desafio.insurance.domain.port.out.InsuranceQuoteRepositoryPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    private static final String CB_NAME = "dynamoDb";

    private final DynamoDbTable<InsuranceQuote> insuranceQuoteTable;

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "saveFallback")
    public InsuranceQuote save(InsuranceQuote quote) {
        quote.preWrite();
        insuranceQuoteTable.putItem(quote);
        log.debug("Quote saved – id: {}", quote.getId());
        return quote;
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "findByIdFallback")
    public Optional<InsuranceQuote> findById(String id, String documentNumber) {
        Key key = Key.builder()
                .partitionValue(id)
                .sortValue(documentNumber)
                .build();
        return Optional.ofNullable(insuranceQuoteTable.getItem(key));
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "updatePolicyIdFallback")
    public InsuranceQuote updatePolicyId(String quoteId, String documentNumber, Long policyId) {
        // Note: internal calls to findById/save bypass AOP proxy (self-invocation),
        // but this method itself is protected by its own circuit breaker.
        Key key = Key.builder().partitionValue(quoteId).sortValue(documentNumber).build();
        InsuranceQuote quote = Optional.ofNullable(insuranceQuoteTable.getItem(key))
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + quoteId));
        quote.setInsurancePolicyId(policyId);
        quote.setStatus(QuoteStatus.APPROVED);
        quote.preWrite();
        insuranceQuoteTable.putItem(quote);
        log.debug("Quote {} updated with policyId {}", quoteId, policyId);
        return quote;
    }

    // ---- fallbacks ------------------------------------------------------

    @SuppressWarnings("unused")
    private InsuranceQuote saveFallback(InsuranceQuote quote, Throwable ex) {
        log.error("Circuit breaker OPEN for DynamoDB (save) – id: {} cause: {}",
                quote.getId(), ex.getMessage());
        throw new ServiceUnavailableException("Banco de dados temporariamente indisponível", ex);
    }

    @SuppressWarnings("unused")
    private Optional<InsuranceQuote> findByIdFallback(String id, String documentNumber, Throwable ex) {
        log.error("Circuit breaker OPEN for DynamoDB (findById) – id: {} cause: {}",
                id, ex.getMessage());
        throw new ServiceUnavailableException("Banco de dados temporariamente indisponível", ex);
    }

    @SuppressWarnings("unused")
    private InsuranceQuote updatePolicyIdFallback(String quoteId, String documentNumber,
                                                  Long policyId, Throwable ex) {
        log.error("Circuit breaker OPEN for DynamoDB (updatePolicyId) – quoteId: {} cause: {}",
                quoteId, ex.getMessage());
        throw new ServiceUnavailableException("Banco de dados temporariamente indisponível", ex);
    }

    // ---- extra queries (not part of the port) ---------------------------

    public List<InsuranceQuote> findByDocumentNumber(String documentNumber) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional
                        .sortBeginsWith(qc -> qc.partitionValue(documentNumber).sortValue(documentNumber)))
                .build();
        return insuranceQuoteTable.query(request).items().stream().collect(Collectors.toList());
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
        return insuranceQuoteTable.scan().items().stream().collect(Collectors.toList());
    }
}
