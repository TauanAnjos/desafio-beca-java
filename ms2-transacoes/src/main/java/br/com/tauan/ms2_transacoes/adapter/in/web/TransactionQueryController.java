package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.adapter.in.web.dto.TransactionResponse;
import br.com.tauan.ms2_transacoes.application.port.in.GetTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsUseCase;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionQueryController {

    private final GetTransactionUseCase getTransaction;
    private final ListTransactionsUseCase listTransactions;

    public TransactionQueryController(GetTransactionUseCase getTransaction,
                                      ListTransactionsUseCase listTransactions) {
        this.getTransaction = getTransaction;
        this.listTransactions = listTransactions;
    }

    /**
     * O dono vem do cabeçalho X-User-Id apenas ENQUANTO a autenticação não existe (DBJ-58 e DBJ-74).
     */
    @GetMapping("/{id}")
    public TransactionResponse get(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return TransactionResponse.from(getTransaction.execute(userId, id));
    }

    @GetMapping
    public List<TransactionResponse> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type) {
        return listTransactions.execute(new ListTransactionsQuery(userId, from, to, type)).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}