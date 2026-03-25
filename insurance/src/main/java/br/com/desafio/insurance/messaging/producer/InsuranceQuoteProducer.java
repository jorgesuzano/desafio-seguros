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
        log.debug("Publishing quote-received event – quoteId: {}", event.getQuoteId());
        try {
            // Serialize event to JSON
            String messageBody = objectMapper.writeValueAsString(event);

            // Send message to SQS
            SendMessageResponse result = sqsClient.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(quoteReceivedQueueUrl)
                            .messageBody(messageBody)
                            .build());
            
            log.debug("Quote-received event sent – quoteId: {} messageId: {}",
                    event.getQuoteId(), result.messageId());

        } catch (Exception e) {
            // Don't rethrow: the quote is already persisted; the event failure is non-fatal.
            log.error("Failed to publish quote-received event for quoteId: {}",
                    event.getQuoteId(), e);
        }
    }
}
