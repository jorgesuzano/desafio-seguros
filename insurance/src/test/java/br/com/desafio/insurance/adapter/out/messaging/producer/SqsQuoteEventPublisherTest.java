package br.com.desafio.insurance.adapter.out.messaging.producer;

import br.com.desafio.insurance.domain.event.InsuranceQuoteReceivedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SqsQuoteEventPublisher}.
 *
 * <p>SqsClient e ObjectMapper são mockados para dar controle total sobre
 * os cenários de sucesso e falha. A URL da fila é injetada via
 * {@link ReflectionTestUtils#setField}, substituindo o {@code @Value}.
 *
 * <p>Nota: a anotação {@code @CircuitBreaker} (Resilience4j AOP) não é
 * ativada em testes unitários sem contexto Spring. Os cenários de
 * circuit-breaker aberto são cobertos pelo teste do método
 * {@code publishFallback}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SqsQuoteEventPublisher")
class SqsQuoteEventPublisherTest {

    @Mock private SqsClient    sqsClient;
    @Mock private ObjectMapper objectMapper;

    private SqsQuoteEventPublisher publisher;

    private static final String QUEUE_URL  = "http://localhost:4566/000000000000/insurance-quote-received";
    private static final String QUOTE_ID   = "quote-xyz-456";
    private static final String MESSAGE_ID = "sqs-msg-001";
    private static final String EVENT_JSON = "{\"quote_id\":\"" + QUOTE_ID + "\"}";

    @BeforeEach
    void setUp() {
        publisher = new SqsQuoteEventPublisher(sqsClient, objectMapper);
        ReflectionTestUtils.setField(publisher, "quoteReceivedQueueUrl", QUEUE_URL);
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    private InsuranceQuoteReceivedEvent buildEvent() {
        return InsuranceQuoteReceivedEvent.builder()
                .quoteId(QUOTE_ID)
                .productId("1b2da7cc-b367-4196-8a78-9cfeec21f587")
                .offerId("adc56d77-348c-4bf0-908f-22d402ee715c")
                .category("HOME")
                .totalMonthlyPremiumAmount(new BigDecimal("75.25"))
                .totalCoverageAmount(new BigDecimal("825000.00"))
                .coverages(Map.of("Incêndio", new BigDecimal("500000.00")))
                .assistances(List.of("Encanador", "Eletricista"))
                .receivedAt("2026-03-25T10:00:00")
                .build();
    }

    private SendMessageResponse successResponse() {
        return SendMessageResponse.builder().messageId(MESSAGE_ID).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  publish – caminho feliz
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("publish – deve enviar mensagem com queueUrl correto")
    void publish_shouldSendMessageToCorrectQueueUrl() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn(EVENT_JSON);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(successResponse());

        publisher.publish(buildEvent());

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
    }

    @Test
    @DisplayName("publish – deve enviar o JSON serializado como messageBody")
    void publish_shouldSendSerializedEventAsMessageBody() throws Exception {
        InsuranceQuoteReceivedEvent event = buildEvent();

        when(objectMapper.writeValueAsString(event)).thenReturn(EVENT_JSON);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(successResponse());

        publisher.publish(event);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().messageBody()).isEqualTo(EVENT_JSON);
    }

    @Test
    @DisplayName("publish – deve serializar o evento com ObjectMapper antes de enviar")
    void publish_shouldSerializeEventWithObjectMapper() throws Exception {
        InsuranceQuoteReceivedEvent event = buildEvent();

        when(objectMapper.writeValueAsString(event)).thenReturn(EVENT_JSON);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(successResponse());

        publisher.publish(event);

        verify(objectMapper).writeValueAsString(event);
    }

    @Test
    @DisplayName("publish – não deve lançar exceção quando SQS retorna resposta válida")
    void publish_shouldNotThrowOnSuccessfulSend() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn(EVENT_JSON);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(successResponse());

        publisher.publish(buildEvent()); // must not throw

        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  publish – erros
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("publish – deve relançar RuntimeException quando sqsClient.sendMessage falha")
    void publish_shouldThrowRuntimeExceptionWhenSqsClientFails() throws Exception {
        InsuranceQuoteReceivedEvent event = buildEvent();

        when(objectMapper.writeValueAsString(any())).thenReturn(EVENT_JSON);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS unavailable"));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(QUOTE_ID);
    }

    @Test
    @DisplayName("publish – deve relançar RuntimeException quando a serialização JSON falha")
    void publish_shouldThrowRuntimeExceptionWhenSerializationFails() throws Exception {
        InsuranceQuoteReceivedEvent event = buildEvent();

        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(QUOTE_ID);
    }

    @Test
    @DisplayName("publish – RuntimeException deve encapsular a causa original")
    void publish_shouldWrapOriginalCauseInRuntimeException() throws Exception {
        InsuranceQuoteReceivedEvent event = buildEvent();
        RuntimeException originalCause = new RuntimeException("Network timeout");

        when(objectMapper.writeValueAsString(any())).thenReturn(EVENT_JSON);
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(originalCause);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasCause(originalCause);
    }

    @Test
    @DisplayName("publish – mensagem de erro deve conter o quoteId para facilitar o rastreio")
    void publish_shouldIncludeQuoteIdInErrorMessage() throws Exception {
        InsuranceQuoteReceivedEvent event = buildEvent();

        when(objectMapper.writeValueAsString(any())).thenReturn(EVENT_JSON);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new RuntimeException("Failure"));

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(QUOTE_ID);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  publishFallback (circuit breaker aberto)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("publishFallback – não deve lançar exceção (apenas loga o aviso)")
    void publishFallback_shouldNotThrowException() {
        InsuranceQuoteReceivedEvent event = buildEvent();
        RuntimeException cause = new RuntimeException("Circuit open");

        // publishFallback é privado – invocado via ReflectionTestUtils
        ReflectionTestUtils.invokeMethod(publisher, "publishFallback", event, cause);

        verifyNoInteractions(sqsClient);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("publishFallback – não deve interagir com SQS quando circuit breaker está aberto")
    void publishFallback_shouldNotCallSqsClient() {
        InsuranceQuoteReceivedEvent event = buildEvent();

        ReflectionTestUtils.invokeMethod(publisher, "publishFallback", event,
                new RuntimeException("Open circuit"));

        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }
}

