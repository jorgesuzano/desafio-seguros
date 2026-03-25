package br.com.desafio.insurance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

@Slf4j
@Configuration
public class AwsSqsConfig {
    
    @Value("${aws.sqs.region:us-east-1}")
    private String awsRegion;
    
    @Bean
    public SqsClient sqsClient() {
        log.info("Configuring SQS client for region: {} and endpoint: http://localhost:4566", awsRegion);

        return SqsClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.of(awsRegion))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("test", "test")
                        )
                )
                .build();
    }

}


