package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ReportResult;
import br.com.tauan.ms2_transacoes.application.port.in.ReportTransactionsUseCase;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportTransactionsUseCase reportTransactions;

    public ReportController(ReportTransactionsUseCase reportTransactions) {
        this.reportTransactions = reportTransactions;
    }

    /**
     * CSV das transações do usuário autenticado. Sem from e to, vale o mês atual.
     */
    @GetMapping("/transactions")
    public ResponseEntity<byte[]> transactions(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type) {
        Long userId = CurrentUser.from(authentication).id();
        ReportResult result = reportTransactions.execute(new ListTransactionsQuery(userId, from, to, type));

        String filename = "transacoes-" + result.from() + "_" + result.to() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(TransactionCsvWriter.write(result.transactions()));
    }
}