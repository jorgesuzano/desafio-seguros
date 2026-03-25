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
    
    @Value("${aws.sqs.endpoint:}")
    private String sqsEndpoint;
    
    @Value("${aws.sqs.region:us-east-1}")
    private String awsRegion;
    
    @Value("${aws.sqs.queue.insurance-quote-received:insurance-quote-received}")
    private String quoteReceivedQueueName;
    
    @Value("${aws.sqs.queue.insurance-policy-created:insurance-policy-created}")
    private String policyCreatedQueueName;

//    @Bean
//    public SqsClient sqsClient() {
//        log.info("Configuring SQS client for region: {}", awsRegion);
//
//        SqsClient sqsClient = SqsClient.builder()
////                .region(Region.SA_EAST_1)
////                .endpointOverride(URI.create(sqsEndpoint))
//                .credentialsProvider(
//                                StaticCredentialsProvider.create(
//                                        AwsBasicCredentials.create("admin", "admin")
//                                )
//                        ).build();
//
//        return sqsClient;
//    }
    
    @Bean
    public SqsClient sqsClient() {
        log.info("Configuring SQS client for region: {}", awsRegion);

//        SqsClientBuilder builder = SqsClient.builder()
//                .region(Region.of(awsRegion))
//            .credentialsProvider(DefaultCredentialsProvider.create());

//        return SqsClient.builder()
//                .endpointOverride(URI.create(sqsEndpoint)) // 👉 LocalStack
//                .region(Region.of(awsRegion))
////                .credentialsProvider(
////                        StaticCredentialsProvider.create(
////                                AwsBasicCredentials.create("admin", "admin")
////                        )
////                )
//                .build();

        SqsClientBuilder builder = SqsClient.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .region(Region.SA_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("admin", "admin")
                        )
                );
        if (sqsEndpoint != null && !sqsEndpoint.isEmpty()) {
            log.info("Using local SQS endpoint: {}", sqsEndpoint);
            builder.endpointOverride(URI.create(sqsEndpoint));
        }
        
        return builder.build();
    }
    
//    @Bean
//    public SqsQueueConfig sqsQueueConfig(SqsClient sqsClient) {
//        return new SqsQueueConfig(
//            sqsClient,
//            quoteReceivedQueueName,
//            policyCreatedQueueName
//        );
//    }
}


