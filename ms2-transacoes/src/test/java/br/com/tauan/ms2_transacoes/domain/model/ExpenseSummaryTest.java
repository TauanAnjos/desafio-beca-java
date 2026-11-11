package br.com.tauan.ms2_transacoes.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseSummaryTest {

    private static final LocalDateTime NOV_10 = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final LocalDateTime NOV_11 = LocalDateTime.of(2026, 11, 11, 15, 30);
    private static final LocalDateTime DEC_01 = LocalDateTime.of(2026, 12, 1, 9, 0);

    private Transaction purchase(String amount, Category category, LocalDateTime when) {
        return Transaction.restore(null, 1L, TransactionType.PURCHASE, new BigDecimal(amount), "Compra",
                category, null, TransactionStatus.APPROVED, null, when);
    }

    private Transaction withdrawal(String amount, LocalDateTime when) {
        return Transaction.restore(null, 1L, TransactionType.WITHDRAWAL, new BigDecimal(amount), "Saque",
                null, null, TransactionStatus.APPROVED, null, when);
    }

    private Transaction transfer(String amount, LocalDateTime when) {
        return Transaction.restore(null, 1L, TransactionType.TRANSFER, new BigDecimal(amount), "Pix",
                null, 2L, TransactionStatus.APPROVED, null, when);
    }

    private Transaction deposit(String amount, LocalDateTime when) {
        return Transaction.restore(null, 1L, TransactionType.DEPOSIT, new BigDecimal(amount), "Salario",
                null, null, TransactionStatus.APPROVED, null, when);
    }

    private Transaction rejectedPurchase(String amount, LocalDateTime when) {
        return Transaction.restore(null, 1L, TransactionType.PURCHASE, new BigDecimal(amount), "Compra",
                Category.FOOD, null, TransactionStatus.REJECTED, "insufficient balance", when);
    }

    @Test
    void fivePurchasesInThreeCategories_showThreeLines_andTheTotalMatchesTheSum() {
        ExpenseSummary summary = ExpenseSummary.of(List.of(
                purchase("10.00", Category.FOOD, NOV_10),
                purchase("20.00", Category.FOOD, NOV_10),
                purchase("30.00", Category.TRANSPORT, NOV_10),
                purchase("40.00", Category.HEALTH, NOV_10),
                purchase("50.00", Category.HEALTH, NOV_10)));

        assertThat(summary.byCategory()).hasSize(3);
        assertThat(summary.byCategory().get(Category.FOOD)).isEqualByComparingTo("30");
        assertThat(summary.byCategory().get(Category.TRANSPORT)).isEqualByComparingTo("30");
        assertThat(summary.byCategory().get(Category.HEALTH)).isEqualByComparingTo("90");
        assertThat(summary.total()).isEqualByComparingTo("150");
    }

    @Test
    void rejectedTransactionsAndDeposits_doNotAppear() {
        ExpenseSummary summary = ExpenseSummary.of(List.of(
                purchase("10.00", Category.FOOD, NOV_10),
                rejectedPurchase("999.00", NOV_10),
                deposit("500.00", NOV_10)));

        assertThat(summary.total()).isEqualByComparingTo("10");
        assertThat(summary.byCategory()).hasSize(1);
    }

    @Test
    void withdrawalsAndSentTransfers_countAsExpenses() {
        ExpenseSummary summary = ExpenseSummary.of(List.of(
                withdrawal("50.00", NOV_10),
                transfer("25.00", NOV_10)));

        assertThat(summary.total()).isEqualByComparingTo("75");
        assertThat(summary.byCategory().get(Category.OTHER)).isEqualByComparingTo("75");
    }

    @Test
    void groupsTheTotalsByDayAndByMonth() {
        ExpenseSummary summary = ExpenseSummary.of(List.of(
                purchase("10.00", Category.FOOD, NOV_10),
                purchase("15.00", Category.FOOD, NOV_10),
                purchase("20.00", Category.FOOD, NOV_11),
                purchase("30.00", Category.FOOD, DEC_01)));

        assertThat(summary.byDay()).hasSize(3);
        assertThat(summary.byDay().get(LocalDate.of(2026, 11, 10))).isEqualByComparingTo("25");
        assertThat(summary.byDay().get(LocalDate.of(2026, 11, 11))).isEqualByComparingTo("20");
        assertThat(summary.byMonth()).hasSize(2);
        assertThat(summary.byMonth().get(YearMonth.of(2026, 11))).isEqualByComparingTo("45");
        assertThat(summary.byMonth().get(YearMonth.of(2026, 12))).isEqualByComparingTo("30");
        assertThat(summary.byDay().firstKey()).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void withoutTransactions_theSummaryIsEmpty() {
        ExpenseSummary summary = ExpenseSummary.of(List.of());

        assertThat(summary.total()).isEqualByComparingTo("0");
        assertThat(summary.byCategory()).isEmpty();
        assertThat(summary.byDay()).isEmpty();
        assertThat(summary.byMonth()).isEmpty();
    }
}