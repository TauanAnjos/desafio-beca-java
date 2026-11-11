package br.com.tauan.ms2_transacoes.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public record ExpenseSummary(BigDecimal total,
                             Map<Category, BigDecimal> byCategory,
                             SortedMap<LocalDate, BigDecimal> byDay,
                             SortedMap<YearMonth, BigDecimal> byMonth) {

    /**
     * Só transações APROVADAS de saída (retirada, transferência enviada e compra) contam como despesa.
     * Depósitos e transações rejeitadas ficam de fora.
     */
    public static ExpenseSummary of(List<Transaction> transactions) {
        BigDecimal total = BigDecimal.ZERO;
        Map<Category, BigDecimal> byCategory = new EnumMap<>(Category.class);
        SortedMap<LocalDate, BigDecimal> byDay = new TreeMap<>();
        SortedMap<YearMonth, BigDecimal> byMonth = new TreeMap<>();

        for (Transaction transaction : transactions) {
            if (transaction.getStatus() != TransactionStatus.APPROVED || !transaction.getType().isExpense()) {
                continue;
            }
            BigDecimal amount = transaction.getAmount();
            total = total.add(amount);
            byCategory.merge(transaction.getCategory(), amount, BigDecimal::add);
            byDay.merge(transaction.getOccurredAt().toLocalDate(), amount, BigDecimal::add);
            byMonth.merge(YearMonth.from(transaction.getOccurredAt()), amount, BigDecimal::add);
        }
        return new ExpenseSummary(total, byCategory, byDay, byMonth);
    }
}