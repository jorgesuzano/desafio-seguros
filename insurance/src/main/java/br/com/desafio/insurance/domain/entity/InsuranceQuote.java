package br.com.desafio.insurance.domain.entity;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@DynamoDbBean
@Builder
public class InsuranceQuote {
    
    // Partition Key
    private String id;
    private String documentNumber;
    private String productId;
    private String offerId;
    private String category;
    private BigDecimal totalMonthlyPremiumAmount;
    private BigDecimal totalCoverageAmount;
    private Map<String, BigDecimal> coverages;
    private List<String> assistances;
    private Customer customer;
    private String customerName;
    private Long insurancePolicyId;
    private QuoteStatus status;
    private Long createdAt;
    private Long updatedAt;
    private String statusIndex;
    private String productOfferIndex;
    
    // Constructors
    public InsuranceQuote() {
    }

    public InsuranceQuote(String id, String documentNumber, String productId, String offerId,
                         String category, BigDecimal totalMonthlyPremiumAmount,
                         BigDecimal totalCoverageAmount, Map<String, BigDecimal> coverages,
                         List<String> assistances, Customer customer, String customerName,
                         Long insurancePolicyId, QuoteStatus status, Long createdAt, Long updatedAt,
                         String statusIndex, String productOfferIndex) {
        this.id = id;
        this.documentNumber = documentNumber;
        this.productId = productId;
        this.offerId = offerId;
        this.category = category;
        this.totalMonthlyPremiumAmount = totalMonthlyPremiumAmount;
        this.totalCoverageAmount = totalCoverageAmount;
        this.coverages = coverages;
        this.assistances = assistances;
        this.customer = customer;
        this.customerName = customerName;
        this.insurancePolicyId = insurancePolicyId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statusIndex = statusIndex;
        this.productOfferIndex = productOfferIndex;
    }
    
    // Lifecycle methods - Call preWrite() before saving
    public void preWrite() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        
        if (this.createdAt == null) {
            this.createdAt = System.currentTimeMillis();
        }
        
        this.updatedAt = System.currentTimeMillis();
        this.statusIndex = status.toString() + "#" + createdAt;
        this.productOfferIndex = productId + "#" + offerId;
    }

    // Getters and Setters
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSortKey
    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    @DynamoDbAttribute("productId")
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    @DynamoDbAttribute("offerId")
    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    @DynamoDbAttribute("category")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @DynamoDbAttribute("totalMonthlyPremiumAmount")
    public BigDecimal getTotalMonthlyPremiumAmount() {
        return totalMonthlyPremiumAmount;
    }

    public void setTotalMonthlyPremiumAmount(BigDecimal totalMonthlyPremiumAmount) {
        this.totalMonthlyPremiumAmount = totalMonthlyPremiumAmount;
    }

    @DynamoDbAttribute("totalCoverageAmount")
    public BigDecimal getTotalCoverageAmount() {
        return totalCoverageAmount;
    }

    public void setTotalCoverageAmount(BigDecimal totalCoverageAmount) {
        this.totalCoverageAmount = totalCoverageAmount;
    }

    @DynamoDbAttribute("coverages")
    public Map<String, BigDecimal> getCoverages() {
        return coverages;
    }

    public void setCoverages(Map<String, BigDecimal> coverages) {
        this.coverages = coverages;
    }

    @DynamoDbAttribute("assistances")
    public List<String> getAssistances() {
        return assistances;
    }

    public void setAssistances(List<String> assistances) {
        this.assistances = assistances;
    }

    @DynamoDbAttribute("customer")
    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @DynamoDbAttribute("customerName")
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    @DynamoDbAttribute("insurancePolicyId")
    public Long getInsurancePolicyId() {
        return insurancePolicyId;
    }

    public void setInsurancePolicyId(Long insurancePolicyId) {
        this.insurancePolicyId = insurancePolicyId;
    }

    @DynamoDbAttribute("status")
    public QuoteStatus getStatus() {
        return status;
    }

    public void setStatus(QuoteStatus status) {
        this.status = status;
    }

    @DynamoDbAttribute("createdAt")
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("statusIndex")
    public String getStatusIndex() {
        return statusIndex;
    }

    public void setStatusIndex(String statusIndex) {
        this.statusIndex = statusIndex;
    }

    @DynamoDbAttribute("productOfferIndex")
    public String getProductOfferIndex() {
        return productOfferIndex;
    }

    public void setProductOfferIndex(String productOfferIndex) {
        this.productOfferIndex = productOfferIndex;
    }
}


