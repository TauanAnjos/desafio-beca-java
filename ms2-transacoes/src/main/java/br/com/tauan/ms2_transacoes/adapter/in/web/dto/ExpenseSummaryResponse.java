package br.com.tauan.ms2_transacoes.adapter.in.web.dto;

import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryResult;
import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.ExpenseSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ExpenseSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal total,
        List<CategoryTotal> byCategory,
        List<DayTotal> byDay,
        List<MonthTotal> byMonth
) {

    public record CategoryTotal(Category category, BigDecimal total) {
    }

    public record DayTotal(LocalDate date, BigDecimal total) {
    }

    public record MonthTotal(String month, BigDecimal total) {
    }

    public static ExpenseSummaryResponse from(ExpenseSummaryResult result) {
        ExpenseSummary summary = result.summary();
        List<CategoryTotal> byCategory = summary.byCategory().entrySet().stream()
                .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
                .map(entry -> new CategoryTotal(entry.getKey(), entry.getValue()))
                .toList();
        List<DayTotal> byDay = summary.byDay().entrySet().stream()
                .map(entry -> new DayTotal(entry.getKey(), entry.getValue()))
                .toList();
        List<MonthTotal> byMonth = summary.byMonth().entrySet().stream()
                .map(entry -> new MonthTotal(entry.getKey().toString(), entry.getValue()))
                .toList();
        return new ExpenseSummaryResponse(result.from(), result.to(), summary.total(),
                byCategory, byDay, byMonth);
    }
}