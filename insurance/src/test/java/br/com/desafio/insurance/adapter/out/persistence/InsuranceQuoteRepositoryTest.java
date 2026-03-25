package br.com.desafio.insurance.adapter.out.persistence;

import br.com.desafio.insurance.domain.exception.ServiceUnavailableException;
import br.com.desafio.insurance.domain.model.InsuranceQuote;
import br.com.desafio.insurance.domain.model.QuoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InsuranceQuoteRepository}.
 *
 * <p>DynamoDbTable is mocked — no real AWS connection.
 * Circuit-breaker AOP is inactive (no Spring context); fallbacks are
 * exercised directly via reflection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceQuoteRepository")
class InsuranceQuoteRepositoryTest {

    @Mock
    private DynamoDbTable<InsuranceQuote> insuranceQuoteTable;

    private InsuranceQuoteRepository repository;

    private static final String QUOTE_ID    = "quote-id-123";
    private static final String DOC_NUMBER  = "36205578900";
    private static final Long   POLICY_ID   = 987654321L;

    @BeforeEach
    void setUp() {
        repository = new InsuranceQuoteRepository(insuranceQuoteTable);
    }

    // ── Fixtures ───────────────────────────────────────────────────────────

    private InsuranceQuote buildQuote() {
        InsuranceQuote q = InsuranceQuote.builder()
                .productId("prod-1")
                .offerId("offer-1")
                .documentNumber(DOC_NUMBER)
                .totalMonthlyPremiumAmount(new BigDecimal("75.25"))
                .totalCoverageAmount(new BigDecimal("825000.00"))
                .status(QuoteStatus.RECEIVED)
                .build();
        return q;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  save()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("save – deve chamar putItem e retornar a cotação com id e timestamps preenchidos")
    void save_shouldCallPutItemAndReturnQuoteWithGeneratedFields() {
        InsuranceQuote quote = buildQuote();
        doNothing().when(insuranceQuoteTable).putItem(any(InsuranceQuote.class));

        InsuranceQuote result = repository.save(quote);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotBlank();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(QuoteStatus.RECEIVED);

        ArgumentCaptor<InsuranceQuote> captor = ArgumentCaptor.forClass(InsuranceQuote.class);
        verify(insuranceQuoteTable).putItem(captor.capture());
        assertThat(captor.getValue().getId()).isNotBlank();
    }

    @Test
    @DisplayName("save – deve retornar a mesma instância de cotação passada como argumento")
    void save_shouldReturnSameInstanceAfterPersisting() {
        InsuranceQuote quote = buildQuote();
        doNothing().when(insuranceQuoteTable).putItem(any(InsuranceQuote.class));

        InsuranceQuote result = repository.save(quote);

        assertThat(result).isSameAs(quote);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  findById()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findById – deve retornar Optional com cotação quando o item existe no DynamoDB")
    void findById_shouldReturnOptionalWithQuoteWhenFound() {
        InsuranceQuote stored = buildQuote();
        when(insuranceQuoteTable.getItem(any(Key.class))).thenReturn(stored);

        Optional<InsuranceQuote> result = repository.findById(QUOTE_ID, DOC_NUMBER);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(stored);
        verify(insuranceQuoteTable).getItem(any(Key.class));
    }

    @Test
    @DisplayName("findById – deve retornar Optional.empty() quando o item não existe no DynamoDB")
    void findById_shouldReturnEmptyOptionalWhenNotFound() {
        when(insuranceQuoteTable.getItem(any(Key.class))).thenReturn(null);

        Optional<InsuranceQuote> result = repository.findById(QUOTE_ID, DOC_NUMBER);

        assertThat(result).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  updatePolicyId()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("updatePolicyId – deve atualizar policyId, mudar status para APPROVED e salvar")
    void updatePolicyId_shouldUpdateStatusAndPolicyIdWhenQuoteExists() {
        InsuranceQuote existing = buildQuote();
        when(insuranceQuoteTable.getItem(any(Key.class))).thenReturn(existing);
        doNothing().when(insuranceQuoteTable).putItem(any(InsuranceQuote.class));

        InsuranceQuote result = repository.updatePolicyId(QUOTE_ID, DOC_NUMBER, POLICY_ID);

        assertThat(result.getInsurancePolicyId()).isEqualTo(POLICY_ID);
        assertThat(result.getStatus()).isEqualTo(QuoteStatus.APPROVED);
        verify(insuranceQuoteTable).putItem(any(InsuranceQuote.class));
    }

    @Test
    @DisplayName("updatePolicyId – deve lançar IllegalArgumentException quando cotação não existe")
    void updatePolicyId_shouldThrowWhenQuoteNotFound() {
        when(insuranceQuoteTable.getItem(any(Key.class))).thenReturn(null);

        assertThatThrownBy(() -> repository.updatePolicyId(QUOTE_ID, DOC_NUMBER, POLICY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quote not found: " + QUOTE_ID);

        verify(insuranceQuoteTable, never()).putItem(any(InsuranceQuote.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Fallbacks (circuit-breaker open)
    //  AOP is inactive in unit tests; fallbacks are tested via reflection.
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("saveFallback – deve lançar ServiceUnavailableException com mensagem correta")
    void saveFallback_shouldThrowServiceUnavailableException() throws Exception {
        InsuranceQuote quote = buildQuote();
        RuntimeException cause = new RuntimeException("DynamoDB connection timeout");

        Method fallback = InsuranceQuoteRepository.class.getDeclaredMethod(
                "saveFallback", InsuranceQuote.class, Throwable.class);
        fallback.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                fallback.invoke(repository, quote, cause);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        })
        .isInstanceOf(ServiceUnavailableException.class)
        .hasMessageContaining("Banco de dados temporariamente indisponível")
        .hasCause(cause);
    }

    @Test
    @DisplayName("findByIdFallback – deve lançar ServiceUnavailableException com mensagem correta")
    void findByIdFallback_shouldThrowServiceUnavailableException() throws Exception {
        RuntimeException cause = new RuntimeException("DynamoDB circuit open");

        Method fallback = InsuranceQuoteRepository.class.getDeclaredMethod(
                "findByIdFallback", String.class, String.class, Throwable.class);
        fallback.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                fallback.invoke(repository, QUOTE_ID, DOC_NUMBER, cause);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        })
        .isInstanceOf(ServiceUnavailableException.class)
        .hasMessageContaining("Banco de dados temporariamente indisponível")
        .hasCause(cause);
    }

    @Test
    @DisplayName("updatePolicyIdFallback – deve lançar ServiceUnavailableException com mensagem correta")
    void updatePolicyIdFallback_shouldThrowServiceUnavailableException() throws Exception {
        RuntimeException cause = new RuntimeException("DynamoDB circuit open");

        Method fallback = InsuranceQuoteRepository.class.getDeclaredMethod(
                "updatePolicyIdFallback", String.class, String.class, Long.class, Throwable.class);
        fallback.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                fallback.invoke(repository, QUOTE_ID, DOC_NUMBER, POLICY_ID, cause);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        })
        .isInstanceOf(ServiceUnavailableException.class)
        .hasMessageContaining("Banco de dados temporariamente indisponível")
        .hasCause(cause);
    }
}

