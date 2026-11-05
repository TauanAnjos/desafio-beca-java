package br.com.tauan.ms2_transacoes.domain.model;

import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionPolicyTest {

    private final TransactionPolicy policy = new TransactionPolicy();

    private AccountInfo account(String balance, String dailyLimit, String perTransactionLimit) {
        return new AccountInfo(1L, new BigDecimal(balance), new BigDecimal(dailyLimit),
                new BigDecimal(perTransactionLimit));
    }

    private TransactionDecision evaluate(TransactionType type, String amount, AccountInfo account, String spentToday) {
        return policy.evaluate(type, new BigDecimal(amount), account,
                spentToday == null ? null : new BigDecimal(spentToday));
    }

    // ---------- depósito ----------

    @Test
    void deposit_isAlwaysApproved_evenWithoutAccount() {
        TransactionDecision decision = evaluate(TransactionType.DEPOSIT, "99999", null, "999999");

        assertThat(decision.approved()).isTrue();
        assertThat(decision.reason()).isNull();
    }

    // ---------- aprovação ----------

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"WITHDRAWAL", "TRANSFER", "PURCHASE"})
    void expense_withinBalanceAndLimits_isApproved(TransactionType type) {
        TransactionDecision decision = evaluate(type, "200", account("1000", "3000", "500"), "100");

        assertThat(decision.approved()).isTrue();
        assertThat(decision.reason()).isNull();
    }

    @Test
    void amountEqualToTheBalance_isApproved() {
        TransactionDecision decision = evaluate(TransactionType.WITHDRAWAL, "400",
                account("400", "3000", "500"), null);

        assertThat(decision.approved()).isTrue();
    }

    @Test
    void sumEqualToTheDailyLimit_isApproved() {
        TransactionDecision decision = evaluate(TransactionType.PURCHASE, "500",
                account("1000", "3000", "500"), "2500");

        assertThat(decision.approved()).isTrue();
    }

    @Test
    void nullSpentToday_isTreatedAsZero() {
        TransactionDecision decision = evaluate(TransactionType.PURCHASE, "500",
                account("1000", "500", "500"), null);

        assertThat(decision.approved()).isTrue();
    }

    // ---------- rejeição ----------

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"WITHDRAWAL", "TRANSFER", "PURCHASE"})
    void expense_greaterThanTheBalance_isRejected(TransactionType type) {
        TransactionDecision decision = evaluate(type, "200", account("100", "3000", "500"), null);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo("insufficient balance");
    }

    @Test
    void purchase_aboveThePerTransactionLimit_isRejected() {
        TransactionDecision decision = evaluate(TransactionType.PURCHASE, "600",
                account("1000", "3000", "500"), null);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo("amount exceeds the per-transaction limit");
    }

    @Test
    void transfer_thatPassesTheDailyLimit_isRejected() {
        TransactionDecision decision = evaluate(TransactionType.TRANSFER, "500",
                account("1000", "3000", "500"), "2600");

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo("daily limit exceeded");
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {"WITHDRAWAL", "TRANSFER", "PURCHASE"})
    void expense_withoutAccount_isRejected(TransactionType type) {
        TransactionDecision decision = evaluate(type, "50", null, null);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.reason()).isEqualTo("account not found for the user");
    }

    @Test
    void whenSeveralRulesFail_theBalanceReasonComesFirst() {
        TransactionDecision decision = evaluate(TransactionType.WITHDRAWAL, "200",
                account("100", "150", "50"), "100");

        assertThat(decision.reason()).isEqualTo("insufficient balance");
    }

    // ---------- entradas inválidas ----------

    @Test
    void nullType_isInvalid() {
        assertThatThrownBy(() -> policy.evaluate(null, new BigDecimal("10"),
                account("100", "300", "50"), null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("type is required");
    }

    @Test
    void nullAmount_isInvalid() {
        assertThatThrownBy(() -> policy.evaluate(TransactionType.WITHDRAWAL, null,
                account("100", "300", "50"), null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("amount must be greater than zero");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void amountNotGreaterThanZero_isInvalid(String amount) {
        assertThatThrownBy(() -> evaluate(TransactionType.WITHDRAWAL, amount,
                account("100", "300", "50"), null))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("amount must be greater than zero");
    }

    // ---------- tipo de transação ----------

    @Test
    void onlyDepositIsNotAnExpense() {
        assertThat(TransactionType.DEPOSIT.isExpense()).isFalse();
        assertThat(TransactionType.WITHDRAWAL.isExpense()).isTrue();
        assertThat(TransactionType.TRANSFER.isExpense()).isTrue();
        assertThat(TransactionType.PURCHASE.isExpense()).isTrue();
    }
}