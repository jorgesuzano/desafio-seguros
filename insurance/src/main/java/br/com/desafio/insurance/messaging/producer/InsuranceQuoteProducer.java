package br.com.desafio.insurance.messaging.producer;

import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsuranceQuoteProducer {
    
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue.insurance-quote-received}")
    private String quoteReceivedQueueName;
    
    public void publishQuoteReceivedEvent(InsuranceQuoteReceivedEvent event) {
        log.info("Publishing InsuranceQuoteReceivedEvent for quote: {}", event.getQuoteId());
        
        try {
            // Initialize queues if not already done
//            if (sqsQueueConfig.getQuoteReceivedQueueUrl() == null) {
//                sqsQueueConfig.initializeQueues();
//            }
            
            // Serialize event to JSON
            String messageBody = objectMapper.writeValueAsString(event);
            
            // Send message to SQS
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(quoteReceivedQueueName)
                .messageBody(messageBody)
                .messageGroupId(event.getQuoteId().toString()) // For FIFO queue consistency
                .messageDeduplicationId(event.getQuoteId().toString() + "-" + System.currentTimeMillis())
                .build();
            
            SendMessageResponse result = sqsClient.sendMessage(sendMsgRequest);
            
            log.info("Quote received event published successfully for quoteId: {} with messageId: {}", 
                event.getQuoteId(), result.messageId());
            
        } catch (Exception e) {
            log.error("Error sending quote received event for quoteId: {}", event.getQuoteId(), e);
            throw new RuntimeException("Failed to publish quote event to SQS", e);
        }
    }
}

