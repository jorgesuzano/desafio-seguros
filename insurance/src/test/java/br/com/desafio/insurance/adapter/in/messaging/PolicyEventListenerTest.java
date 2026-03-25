package br.com.desafio.insurance.adapter.in.messaging;

import br.com.desafio.insurance.domain.event.InsurancePolicyCreatedEvent;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.model.QuoteStatus;
import br.com.desafio.insurance.domain.port.in.PolicyUpdateUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PolicyEventListener}.
 *
 * <p>SqsClient and PolicyUpdateUseCase are mocked.
 * ObjectMapper é real para exercitar o caminho de desserialização JSON.
 * {@code @Scheduled} e {@code @Value} estão inativos sem contexto Spring;
 * a URL da fila é injetada via {@link ReflectionTestUtils#setField}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyEventListener")
class PolicyEventListenerTest {

    @Mock private PolicyUpdateUseCase policyUpdateUseCase;
    @Mock private SqsClient           sqsClient;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private PolicyEventListener listener;

    private static final String QUEUE_URL  = "http://localhost:4566/000000000000/insurance-policy-created";
    private static final String QUOTE_ID   = "quote-abc-123";
    private static final String DOC_NUMBER = "36205578900";
    private static final Long   POLICY_ID  = 987654321L;
    private static final String RECEIPT    = "receipt-handle-xyz";
    private static final String MESSAGE_ID = "msg-id-001";

    @BeforeEach
    void setUp() {
        listener = new PolicyEventListener(policyUpdateUseCase, sqsClient, objectMapper);
        ReflectionTestUtils.setField(listener, "policyCreatedQueueUrl", QUEUE_URL);
    }

    // ── Fixtures ───────────────────────────────────────────────────────────

    private String validEventJson() throws Exception {
        InsurancePolicyCreatedEvent event = InsurancePolicyCreatedEvent.builder()
                .policyId(POLICY_ID)
                .quoteId(QUOTE_ID)
                .documentNumber(DOC_NUMBER)
                .issuedAt(LocalDateTime.of(2026, 3, 25, 10, 0))
                .build();
        return objectMapper.writeValueAsString(event);
    }

    private Message buildMessage(String body) {
        return Message.builder()
                .messageId(MESSAGE_ID)
                .receiptHandle(RECEIPT)
                .body(body)
                .build();
    }

    private ReceiveMessageResponse emptyResponse() {
        return ReceiveMessageResponse.builder().messages(List.of()).build();
    }

    private ReceiveMessageResponse responseWith(Message... messages) {
        return ReceiveMessageResponse.builder().messages(messages).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  poll – fila vazia
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("poll – não deve executar nada quando a fila retorna lista vazia")
    void poll_shouldDoNothingWhenNoMessagesReceived() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(emptyResponse());

        listener.pollInsurancePolicyCreatedEvents();

        verifyNoInteractions(policyUpdateUseCase);
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  poll – mensagem válida
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("poll – mensagem válida: deve atualizar cotação e deletar mensagem da fila")
    void poll_shouldUpdateQuoteAndDeleteMessageOnValidEvent() throws Exception {
        InsuranceQuote updated = InsuranceQuote.builder()
                .documentNumber(DOC_NUMBER).status(QuoteStatus.APPROVED).build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(responseWith(buildMessage(validEventJson())));
        when(policyUpdateUseCase.updateQuoteWithPolicyId(QUOTE_ID, DOC_NUMBER, POLICY_ID))
                .thenReturn(updated);
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        listener.pollInsurancePolicyCreatedEvents();

        verify(policyUpdateUseCase).updateQuoteWithPolicyId(QUOTE_ID, DOC_NUMBER, POLICY_ID);

        ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(captor.getValue().receiptHandle()).isEqualTo(RECEIPT);
    }

    @Test
    @DisplayName("poll – campos corretos são passados para updateQuoteWithPolicyId")
    void poll_shouldPassCorrectFieldsToUseCase() throws Exception {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(responseWith(buildMessage(validEventJson())));
        when(policyUpdateUseCase.updateQuoteWithPolicyId(any(), any(), any()))
                .thenReturn(InsuranceQuote.builder().build());
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        listener.pollInsurancePolicyCreatedEvents();

        ArgumentCaptor<String> quoteIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> docCaptor     = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long>   policyCaptor  = ArgumentCaptor.forClass(Long.class);
        verify(policyUpdateUseCase).updateQuoteWithPolicyId(
                quoteIdCaptor.capture(), docCaptor.capture(), policyCaptor.capture());

        assertThat(quoteIdCaptor.getValue()).isEqualTo(QUOTE_ID);
        assertThat(docCaptor.getValue()).isEqualTo(DOC_NUMBER);
        assertThat(policyCaptor.getValue()).isEqualTo(POLICY_ID);
    }

    @Test
    @DisplayName("poll – múltiplas mensagens: deve processar todas e deletar cada uma")
    void poll_shouldProcessAllMessagesInBatch() throws Exception {
        Message msg2 = Message.builder()
                .messageId("msg-002").receiptHandle("receipt-002")
                .body(objectMapper.writeValueAsString(
                        InsurancePolicyCreatedEvent.builder()
                                .policyId(111L).quoteId("q2").documentNumber("99999999999")
                                .issuedAt(LocalDateTime.now()).build()))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(responseWith(buildMessage(validEventJson()), msg2));
        when(policyUpdateUseCase.updateQuoteWithPolicyId(any(), any(), any()))
                .thenReturn(InsuranceQuote.builder().build());
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        listener.pollInsurancePolicyCreatedEvents();

        verify(policyUpdateUseCase, times(2)).updateQuoteWithPolicyId(any(), any(), any());
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  poll – erros de processamento de mensagem
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("poll – JSON malformado: deve deletar mensagem e NÃO chamar o use-case")
    void poll_shouldDeleteAndSkipUseCaseOnMalformedJson() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(responseWith(buildMessage("{ not valid json }")));
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        listener.pollInsurancePolicyCreatedEvents();

        verifyNoInteractions(policyUpdateUseCase);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("poll – use-case lança exceção: mensagem NÃO deve ser deletada (fica disponível para retry)")
    void poll_shouldNotDeleteMessageWhenUseCaseThrows() throws Exception {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(responseWith(buildMessage(validEventJson())));
        when(policyUpdateUseCase.updateQuoteWithPolicyId(any(), any(), any()))
                .thenThrow(new RuntimeException("DynamoDB unavailable"));

        listener.pollInsurancePolicyCreatedEvents();

        verify(policyUpdateUseCase).updateQuoteWithPolicyId(any(), any(), any());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    @DisplayName("poll – falha ao deletar mensagem: não deve propagar a exceção")
    void poll_shouldNotPropagateDeleteMessageFailure() throws Exception {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(responseWith(buildMessage(validEventJson())));
        when(policyUpdateUseCase.updateQuoteWithPolicyId(any(), any(), any()))
                .thenReturn(InsuranceQuote.builder().build());
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenThrow(new RuntimeException("SQS delete failed"));

        listener.pollInsurancePolicyCreatedEvents(); // must not throw

        verify(policyUpdateUseCase).updateQuoteWithPolicyId(any(), any(), any());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  poll – erros do SQS
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("poll – SqsException status 500: deve absorver a exceção sem propagar")
    void poll_shouldHandleSqsException500WithoutRethrow() {
        SqsException sqsEx = (SqsException) SqsException.builder()
                .statusCode(500).message("Internal Server Error").build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(sqsEx);

        listener.pollInsurancePolicyCreatedEvents(); // must not throw

        verifyNoInteractions(policyUpdateUseCase);
    }

    @Test
    @DisplayName("poll – SqsException status 400: deve absorver a exceção sem propagar")
    void poll_shouldHandleSqsException400WithoutRethrow() {
        SqsException sqsEx = (SqsException) SqsException.builder()
                .statusCode(400).message("Bad Request").build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(sqsEx);

        listener.pollInsurancePolicyCreatedEvents(); // must not throw

        verifyNoInteractions(policyUpdateUseCase);
    }

    @Test
    @DisplayName("poll – RuntimeException inesperada: deve absorver e não propagar")
    void poll_shouldHandleUnexpectedExceptionWithoutRethrow() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(new RuntimeException("Unexpected failure"));

        listener.pollInsurancePolicyCreatedEvents(); // must not throw

        verifyNoInteractions(policyUpdateUseCase);
    }
}
