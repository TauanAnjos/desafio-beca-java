package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.adapter.in.web.dto.ExpenseSummaryResponse;
import br.com.tauan.ms2_transacoes.application.port.in.ExpenseSummaryQuery;
import br.com.tauan.ms2_transacoes.application.port.in.GetExpenseSummaryUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final GetExpenseSummaryUseCase getExpenseSummary;

    public ExpenseController(GetExpenseSummaryUseCase getExpenseSummary) {
        this.getExpenseSummary = getExpenseSummary;
    }

    @GetMapping("/summary")
    public ExpenseSummaryResponse summary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = CurrentUser.from(authentication).id();
        return ExpenseSummaryResponse.from(getExpenseSummary.execute(new ExpenseSummaryQuery(userId, from, to)));
    }
}