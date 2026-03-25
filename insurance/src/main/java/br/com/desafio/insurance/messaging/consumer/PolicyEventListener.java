package br.com.desafio.insurance.messaging.consumer;

import br.com.desafio.insurance.domain.event.InsurancePolicyCreatedEvent;
import br.com.desafio.insurance.service.InsuranceQuoteServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final InsuranceQuoteServicePort quoteService;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.fila.insurance-policy-created}")
    private String policyCreatedQueueUrl;

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void pollInsurancePolicyCreatedEvents() {
        try {
            List<Message> messages = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(policyCreatedQueueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(5)
                            .visibilityTimeout(30)
                            .build()
            ).messages();

            if (!messages.isEmpty()) {
                log.debug("Received {} policy-created message(s)", messages.size());
                messages.forEach(m -> processMessage(m, policyCreatedQueueUrl));
            }

        } catch (SqsException e) {
            if (e.statusCode() == 500) {
                log.warn("SQS temporarily unavailable – retrying on next poll");
            } else {
                log.error("SQS error ({}): {}", e.statusCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error polling policy-created queue", e);
        }
    }

    private void processMessage(Message message, String queueUrl) {
        try {
            InsurancePolicyCreatedEvent event = objectMapper.readValue(
                    message.body(), InsurancePolicyCreatedEvent.class);

            quoteService.updateQuoteWithPolicyId(
                    event.getQuoteId(),
                    event.getDocumentNumber(),
                    event.getPolicyId());

            log.debug("Quote {} updated with policy {}", event.getQuoteId(), event.getPolicyId());
            deleteMessage(queueUrl, message.receiptHandle());

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Malformed policy-created message {}: {}", message.messageId(), e.getMessage());
            deleteMessage(queueUrl, message.receiptHandle());
        } catch (Exception e) {
            log.error("Error processing policy-created message {}", message.messageId(), e);
        }
    }

    private void deleteMessage(String queueUrl, String receiptHandle) {
        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl).receiptHandle(receiptHandle).build());
        } catch (Exception e) {
            log.warn("Failed to delete SQS message: {}", e.getMessage());
        }
    }
}
