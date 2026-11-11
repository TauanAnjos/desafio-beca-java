package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.adapter.in.web.dto.TransactionResponse;
import br.com.tauan.ms2_transacoes.application.port.in.GetTransactionUseCase;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsUseCase;
import br.com.tauan.ms2_transacoes.domain.exception.ForbiddenAccessException;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    public TransactionResponse get(Authentication authentication, @PathVariable Long id) {
        return TransactionResponse.from(getTransaction.execute(CurrentUser.from(authentication).id(), id));
    }

    /**
     * Lista só as transações do usuário autenticado. O parâmetro opcional userId existe para deixar
     * explícito o bloqueio: pedir as transações de outro usuário responde 403.
     */
    @GetMapping
    public List<TransactionResponse> list(
            Authentication authentication,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) TransactionType type) {
        AuthenticatedUser user = CurrentUser.from(authentication);
        if (userId != null && !userId.equals(user.id())) {
            throw new ForbiddenAccessException("You can only access your own transactions");
        }
        return listTransactions.execute(new ListTransactionsQuery(user.id(), from, to, type)).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}