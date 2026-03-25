package br.com.desafio.insurance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Slf4j
@Configuration
public class AwsSqsConfig {
    
    @Value("${aws.sqs.region:sa-east-1}")
    private String awsRegion;

    @Value("${aws.sqs.endpoint:http://localhost:4566}")
    private String endpoint;

    @Bean
    public SqsClient sqsClient() {
        log.info("Configuring SQS client for region: {} and endpoint: {}", awsRegion, endpoint);

        var credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create("admin", "admin")
        );
        // Using empty credentials for LocalStack - it accepts any credentials in local mode
        var builder = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider);

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

}


