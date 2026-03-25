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
    
    @Bean
    public SqsClient sqsClient() {
        log.info("Configuring SQS client for region: {} and endpoint: http://localhost:4566", awsRegion);

        // Using empty credentials for LocalStack - it accepts any credentials in local mode
        return SqsClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.of(awsRegion))
//                .credentialsProvider(
//                        StaticCredentialsProvider.create(
//                                AwsBasicCredentials.create("admin", "admin")
//                        )
//                )
                .build();
    }

}


