package br.com.desafio.insurance.adapter.out.messaging.producer;

import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import br.com.desafio.insurance.domain.port.out.QuoteEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    private static final String CB_NAME = "sqsPublisher";

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.fila.insurance-quote-received}")
    private String quoteReceivedQueueUrl;

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "publishFallback")
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
            // Re-throw so Resilience4j can record the failure and open the circuit.
            throw new RuntimeException("Failed to publish quote-received event for quoteId: "
                    + event.getQuoteId(), e);
        }
    }

    // ---- fallback -------------------------------------------------------

    @SuppressWarnings("unused")
    private void publishFallback(InsuranceQuoteReceivedEvent event, Throwable ex) {
        log.warn("Circuit breaker OPEN for SQS publisher – quoteId: {} cause: {}",
                event.getQuoteId(), ex.getMessage());
    }
}
