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

    @Value("${cloud.aws.fila.insurance-quote-received}")
    private String quoteReceivedQueueUrl;
    
    public void publishQuoteReceivedEvent(InsuranceQuoteReceivedEvent event) {
        log.info("Publishing InsuranceQuoteReceivedEvent for quote: {}", event.getQuoteId());
        
        try {
            // Serialize event to JSON
            String messageBody = objectMapper.writeValueAsString(event);
            
            log.debug("Queue URL: {}", quoteReceivedQueueUrl);
            log.debug("Message body: {}", messageBody);
            
            // Send message to SQS
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(quoteReceivedQueueUrl)
                .messageBody(messageBody)
                .build();
            
            SendMessageResponse result = sqsClient.sendMessage(sendMsgRequest);
            
            log.info("Quote received event published successfully for quoteId: {} with messageId: {}", 
                event.getQuoteId(), result.messageId());
            
        } catch (Exception e) {
            log.error("Error sending quote received event for quoteId: {}. Queue URL: {}", 
                event.getQuoteId(), quoteReceivedQueueUrl, e);
            // Log but don't rethrow - the quote is already persisted in DynamoDB
        }
    }
}

