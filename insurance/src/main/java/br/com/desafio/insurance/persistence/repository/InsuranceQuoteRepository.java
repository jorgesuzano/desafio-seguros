package br.com.desafio.insurance.persistence.repository;

import br.com.desafio.insurance.domain.entity.InsuranceQuote;
import br.com.desafio.insurance.domain.entity.QuoteStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InsuranceQuoteRepository {
    
    private final DynamoDbTable<InsuranceQuote> insuranceQuoteTable;
    
    public InsuranceQuote save(InsuranceQuote quote) {
        log.debug("Saving insurance quote with ID: {}", quote.getId());
        // Call preWrite to generate ID and timestamps
        quote.preWrite();
        insuranceQuoteTable.putItem(quote);
        return quote;
    }
    
    public Optional<InsuranceQuote> findById(String id, String documentNumber) {
        log.debug("Finding quote by ID: {} and documentNumber: {}", id, documentNumber);
        
        Key key = Key.builder()
            .partitionValue(id)
            .sortValue(documentNumber)
            .build();
        
        InsuranceQuote quote = insuranceQuoteTable.getItem(key);
        return Optional.ofNullable(quote);
    }
    
    public Optional<InsuranceQuote> findById(Long quoteId) {
        // This method is for backward compatibility - convert Long to String ID
        // In a real scenario, you'd need to know the documentNumber to query
        log.warn("Using findById with Long - this requires documentNumber for DynamoDB query");
        return Optional.empty();
    }
    
//    public List<InsuranceQuote> findByStatus(QuoteStatus status) {
//        log.debug("Finding quotes by status: {}", status);
//
//        Expression filterExpression = Expression.builder()
//            .expression("#status = :statusValue")
//            .putExpressionName("#status", "status")
//            .putExpressionValue(":statusValue", AttributeValue.builder().s(status.toString())
//            .build()).build();
//
//
//        return insuranceQuoteTable.scan().
//            .filterExpression(filterExpression)
//            .items()
//            .stream()
//            .collect(Collectors.toList());
//    }
    
    public List<InsuranceQuote> findByDocumentNumber(String documentNumber) {
        log.debug("Finding quotes by documentNumber: {}", documentNumber);
        
        // Query using the sort key (documentNumber)
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional
                    .sortBeginsWith(qc -> qc.partitionValue(documentNumber).sortValue(documentNumber)))
            .build();
        
        return insuranceQuoteTable.query(request)
            .items()
            .stream()
            .collect(Collectors.toList());
    }
    
//    public List<InsuranceQuote> findByCreatedAtBetween(long startDateMs, long endDateMs) {
//        log.debug("Finding quotes created between {} and {}", startDateMs, endDateMs);
//
//        Expression filterExpression = Expression.builder()
//            .expression("createdAt BETWEEN :startDate AND :endDate")
//            .value(":startDate", startDateMs)
//            .value(":endDate", endDateMs)
//            .build();
//
//        return insuranceQuoteTable.scan()
//            .filterExpression(filterExpression)
//            .items()
//            .stream()
//            .collect(Collectors.toList());
//    }
    
    public Optional<InsuranceQuote> findByIdAndDocumentNumber(Long quoteId, String documentNumber) {
        return findById(quoteId.toString(), documentNumber);
    }
    
//    public Optional<InsuranceQuote> findByProductIdAndOfferId(String productId, String offerId) {
//        log.debug("Finding quote by productId: {} and offerId: {}", productId, offerId);
//
//        Expression filterExpression = Expression.builder()
//            .expression("productId = :productId AND offerId = :offerId")
//            .value(":productId", productId)
//            .value(":offerId", offerId)
//            .build();
//
//        List<InsuranceQuote> results = insuranceQuoteTable.scan()
//            .filterExpression(filterExpression)
//            .items()
//            .stream()
//            .collect(Collectors.toList());
//
//        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
//    }
    
    public void delete(InsuranceQuote quote) {
        log.debug("Deleting quote with ID: {}", quote.getId());
        
        Key key = Key.builder()
            .partitionValue(quote.getId())
            .sortValue(quote.getDocumentNumber())
            .build();
        
        insuranceQuoteTable.deleteItem(key);
    }
    
    public void deleteById(String id, String documentNumber) {
        log.debug("Deleting quote by ID: {} and documentNumber: {}", id, documentNumber);
        
        Key key = Key.builder()
            .partitionValue(id)
            .sortValue(documentNumber)
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
    
    public long count() {
        log.debug("Counting all quotes");
        return findAll().size();
    }
}


