package br.com.desafio.insurance.messaging.consumer;

import br.com.desafio.insurance.domain.event.InsurancePolicyCreatedEvent;
import br.com.desafio.insurance.service.InsuranceQuoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;


import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PolicyEventListener {
    
    private final InsuranceQuoteService quoteService;
    private final SqsClient sqsClient;
//    private final SqsQueueConfig sqsQueueConfig;
    private final ObjectMapper objectMapper;
    
    private volatile boolean initialized = false;

    @Value("${cloud.aws.fila.insurance-policy-created}")
    private String policyCreatedQueueName;
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeQueues() {
        log.info("Initializing SQS queues on application startup");
//        sqsQueueConfig.initializeQueues();
//        initialized = true;
    }
    
    /**
     * Polls SQS queue for policy created events every 5 seconds
     * This is scheduled to run asynchronously
     */
    @Async
    public void pollInsurancePolicyCreatedEvents() {
//        if (!initialized) {
//            log.debug("Queues not initialized yet, skipping poll");
//            return;
//        }

        try {
            String queueUrl = policyCreatedQueueName;

            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10) // Process up to 10 messages per poll
                .waitTimeSeconds(20) // Long polling
                .visibilityTimeout(30)
                .messageAttributeNames("All")
                .build();

            ReceiveMessageResponse receiveResult = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = receiveResult.messages();

            if (!messages.isEmpty()) {
                log.info("Received {} policy created event(s) from SQS", messages.size());
            }

            for (Message message : messages) {
                processMessage(message, queueUrl);
            }

        } catch (Exception e) {
            log.error("Error polling insurance policy created events from SQS", e);
        }
    }
    
    private void processMessage(Message message, String queueUrl) {
        String messageId = message.messageId();
        String receiptHandle = message.receiptHandle();
        
        try {
            log.debug("Processing SQS message: {}", messageId);
            
            // Deserialize message body to event
            InsurancePolicyCreatedEvent event = objectMapper.readValue(
                message.body(),
                InsurancePolicyCreatedEvent.class
            );
            
            log.info("Received insurance policy created event for quote: {} with policy: {}", 
                event.getQuoteId(), event.getPolicyId());
            
            // Update quote with policy ID using String-based method
            quoteService.updateQuoteWithPolicyId(
                event.getQuoteId(), 
                event.getDocumentNumber(), 
                event.getPolicyId()
            );
            
            log.info("Quote {} updated successfully with policy ID: {}", 
                event.getQuoteId(), event.getPolicyId());
            
            // Delete message from queue after successful processing
            deleteMessage(queueUrl, receiptHandle);
            
        } catch (Exception e) {
            log.error("Error processing policy created event from SQS message: {}", messageId, e);
            // Message will be retried after visibility timeout expires
        }
    }
    
    private void deleteMessage(String queueUrl, String receiptHandle) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
            
            sqsClient.deleteMessage(deleteRequest);
            log.debug("Message deleted from queue successfully");
            
        } catch (Exception e) {
            log.error("Error deleting message from SQS queue", e);
        }
    }
}




