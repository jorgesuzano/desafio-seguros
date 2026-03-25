package br.com.desafio.insurance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import br.com.desafio.insurance.domain.entity.InsuranceQuote;

import java.net.URI;

@Slf4j
@Configuration
public class DynamoDbConfig {
    
    @Value("${aws.dynamodb.region:sa-east-1}")
    private String awsRegion;
    
    @Value("${aws.dynamodb.table.name:insurance-quotes}")
    private String tableName;
    
    @Bean
    public DynamoDbClient dynamoDbClient() {
        log.info("Configuring DynamoDB client for region: {} and table: {}", awsRegion, tableName);
        
        return DynamoDbClient.builder()
            .endpointOverride(URI.create("http://localhost:4566"))
            .region(Region.of(awsRegion))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")
            ))
            .build();
    }
    
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();
    }
    
    @Bean
    public DynamoDbTable<InsuranceQuote> insuranceQuoteTable(
        DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        
        log.info("Creating DynamoDbTable bean for InsuranceQuote with table name: {}", tableName);
        
        return dynamoDbEnhancedClient.table(
            tableName,
            TableSchema.fromClass(InsuranceQuote.class)
        );
    }
}

