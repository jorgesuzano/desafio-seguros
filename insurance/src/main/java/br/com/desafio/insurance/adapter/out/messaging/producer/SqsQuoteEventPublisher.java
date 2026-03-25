package br.com.desafio.insurance.adapter.out.messaging.producer;

import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.domain.port.out.QuoteEventPublisherPort;
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
public class SqsQuoteEventPublisher implements QuoteEventPublisherPort {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.fila.insurance-quote-received}")
    private String quoteReceivedQueueUrl;

    @Override
    public void publish(InsuranceQuoteReceivedEvent event) {
        log.debug("Publishing quote-received event – quoteId: {}", event.getQuoteId());
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            SendMessageResponse result = sqsClient.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(quoteReceivedQueueUrl)
                            .messageBody(messageBody)
                            .build());
            log.debug("Quote-received event sent – quoteId: {} messageId: {}",
                    event.getQuoteId(), result.messageId());
        } catch (Exception e) {
            // Non-fatal: quote is already persisted; event failure should not roll back.
            log.error("Failed to publish quote-received event for quoteId: {}",
                    event.getQuoteId(), e);
        }
    }
}

