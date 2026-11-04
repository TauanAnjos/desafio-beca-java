package br.com.tauan.ms2_transacoes.domain.model;

import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);

    private Transaction approved(TransactionType type, String amount, String description,
                                 Category category, Long destinationUserId) {
        return Transaction.approved(1L, type, new BigDecimal(amount), description, category,
                destinationUserId, NOW);
    }

    private Transaction rejected(String reason) {
        return Transaction.rejected(1L, TransactionType.WITHDRAWAL, new BigDecimal("500"), "Saque",
                null, null, NOW, reason);
    }

    // ---------- criação válida ----------

    @Test
    void approvedDeposit_hasDefaultCategoryAndTwoDecimals() {
        Transaction transaction = approved(TransactionType.DEPOSIT, "100", "Salário", null, null);

        assertThat(transaction.getId()).isNull();
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(transaction.getCategory()).isEqualTo(Category.OTHER);
        assertThat(transaction.getAmount().toPlainString()).isEqualTo("100.00");
        assertThat(transaction.getRejectionReason()).isNull();
        assertThat(transaction.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void rejectedWithdrawal_keepsTheTrimmedReason() {
        Transaction transaction = rejected("  insufficient balance ");

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(transaction.getRejectionReason()).isEqualTo("insufficient balance");
    }

    @Test
    void restore_keepsTheId() {
        Transaction transaction = Transaction.restore(7L, 1L, TransactionType.DEPOSIT, new BigDecimal("10.00"),
                "Pix", Category.OTHER, null, TransactionStatus.APPROVED, null, NOW);

        assertThat(transaction.getId()).isEqualTo(7L);
    }

    // ---------- compra ----------

    @Test
    void purchase_requiresCategory() {
        assertThatThrownBy(() -> approved(TransactionType.PURCHASE, "50", "Mercado", null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("category is required for purchases");
    }

    @Test
    void purchase_keepsTheInformedCategory() {
        Transaction transaction = approved(TransactionType.PURCHASE, "50", "Mercado", Category.FOOD, null);

        assertThat(transaction.getCategory()).isEqualTo(Category.FOOD);
    }

    // ---------- transferência ----------

    @Test
    void transfer_requiresDestinationUser() {
        assertThatThrownBy(() -> approved(TransactionType.TRANSFER, "50", "Pix", null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("destination user is required for transfers");
    }

    @Test
    void transfer_toTheOwner_isRejected() {
        assertThatThrownBy(() -> approved(TransactionType.TRANSFER, "50", "Pix", null, 1L))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("destination user must be different from the owner");
    }

    @Test
    void transfer_withDestination_isValid() {
        Transaction transaction = approved(TransactionType.TRANSFER, "50", "Pix", null, 2L);

        assertThat(transaction.getDestinationUserId()).isEqualTo(2L);
    }

    @Test
    void destinationUser_isOnlyAllowedForTransfers() {
        assertThatThrownBy(() -> approved(TransactionType.WITHDRAWAL, "50", "Saque", null, 2L))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("destination user is only allowed for transfers");
    }

    // ---------- valor ----------

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10", "-0.01"})
    void amountNotGreaterThanZero_isRejected(String amount) {
        assertThatThrownBy(() -> approved(TransactionType.DEPOSIT, amount, "Pix", null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("amount must be greater than zero");
    }

    @Test
    void nullAmount_isRejected() {
        assertThatThrownBy(() -> Transaction.approved(1L, TransactionType.DEPOSIT, null, "Pix", null, null, NOW))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("amount must be greater than zero");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.999", "0.001"})
    void amountWithMoreThanTwoDecimals_isRejected(String amount) {
        assertThatThrownBy(() -> approved(TransactionType.DEPOSIT, amount, "Pix", null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("amount must have at most 2 decimal places");
    }

    @Test
    void amountWithTrailingZeros_isAcceptedAndNormalized() {
        Transaction transaction = approved(TransactionType.DEPOSIT, "10.500", "Pix", null, null);

        assertThat(transaction.getAmount().toPlainString()).isEqualTo("10.50");
    }

    // ---------- descrição ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankDescription_isRejected(String description) {
        assertThatThrownBy(() -> approved(TransactionType.DEPOSIT, "10", description, null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("description is required");
    }

    @Test
    void descriptionOver255Characters_isRejected() {
        assertThatThrownBy(() -> approved(TransactionType.DEPOSIT, "10", "a".repeat(256), null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("description must have at most 255 characters");
    }

    @Test
    void description_isTrimmed_andAccepts255Characters() {
        Transaction transaction = approved(TransactionType.DEPOSIT, "10", "  " + "a".repeat(255) + "  ", null, null);

        assertThat(transaction.getDescription()).hasSize(255);
    }

    // ---------- campos obrigatórios ----------

    @Test
    void nullUser_isRejected() {
        assertThatThrownBy(() -> Transaction.approved(null, TransactionType.DEPOSIT, new BigDecimal("10"),
                "Pix", null, null, NOW))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("user is required");
    }

    @Test
    void nullType_isRejected() {
        assertThatThrownBy(() -> Transaction.approved(1L, null, new BigDecimal("10"), "Pix", null, null, NOW))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("type is required");
    }

    @Test
    void nullDateTime_isRejected() {
        assertThatThrownBy(() -> Transaction.approved(1L, TransactionType.DEPOSIT, new BigDecimal("10"),
                "Pix", null, null, null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("date and time are required");
    }

    // ---------- motivo da recusa ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rejectedWithoutReason_isInvalid(String reason) {
        assertThatThrownBy(() -> rejected(reason))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("rejection reason is required");
    }

    @Test
    void approvedWithReason_isInvalid() {
        assertThatThrownBy(() -> Transaction.restore(1L, 1L, TransactionType.DEPOSIT, new BigDecimal("10"),
                "Pix", null, null, TransactionStatus.APPROVED, "algum motivo", NOW))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("rejection reason is only allowed for rejected transactions");
    }
}