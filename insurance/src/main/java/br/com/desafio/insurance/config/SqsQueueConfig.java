//package br.com.desafio.insurance.config;
//
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import software.amazon.awssdk.services.sqs.SqsClient;
//import software.amazon.awssdk.services.sqs.model.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//@Getter
//@RequiredArgsConstructor
//public class SqsQueueConfig {
//
//    private final SqsClient sqsClient;
//    private final String quoteReceivedQueueName;
//    private final String policyCreatedQueueName;
//
//    private String quoteReceivedQueueUrl;
//    private String policyCreatedQueueUrl;
//
//    public void initializeQueues() {
//        try {
//            log.info("Initializing SQS queues...");
//
//            // Get or create quote received queue
//            quoteReceivedQueueUrl = getOrCreateQueue(quoteReceivedQueueName);
//            log.info("Insurance Quote Received Queue URL: {}", quoteReceivedQueueUrl);
//
//            // Get or create policy created queue
//            policyCreatedQueueUrl = getOrCreateQueue(policyCreatedQueueName);
//            log.info("Insurance Policy Created Queue URL: {}", policyCreatedQueueUrl);
//
//        } catch (Exception e) {
//            log.error("Error initializing SQS queues", e);
//            throw new RuntimeException("Failed to initialize SQS queues", e);
//        }
//    }
//
//    private String getOrCreateQueue(String queueName) {
//        try {
//            // Try to get existing queue
//            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
//                .queueName(queueName)
//                .build();
//
//            GetQueueUrlResponse getQueueResponse = sqsClient.getQueueUrl(getQueueRequest);
//            log.debug("Queue {} already exists: {}", queueName, getQueueResponse.queueUrl());
//            return getQueueResponse.queueUrl();
//
//        } catch (QueueDoesNotExistException e) {
//            // Create new queue if it doesn't exist
//            log.info("Creating new SQS queue: {}", queueName);
//
//            // Create attributes map
//            Map<QueueAttributeName, String> attributes = new HashMap<>();
//            attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");
//            attributes.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, "1209600"); // 14 days
//            attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "20"); // Long polling
//
//            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
//                .queueName(queueName)
//                .attributes(attributes)
//                .build();
//
//            CreateQueueResponse createQueueResponse = sqsClient.createQueue(createQueueRequest);
//            log.info("Queue {} created successfully: {}", queueName, createQueueResponse.queueUrl());
//            return createQueueResponse.queueUrl();
//        }
//    }
//}
//
