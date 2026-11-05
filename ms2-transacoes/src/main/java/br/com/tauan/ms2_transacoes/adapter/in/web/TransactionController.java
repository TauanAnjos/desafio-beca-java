package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.adapter.in.web.dto.TransactionRequest;
import br.com.tauan.ms2_transacoes.adapter.in.web.dto.TransactionResponse;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionCommand;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final CreateTransactionUseCase createTransaction;

    public TransactionController(CreateTransactionUseCase createTransaction) {
        this.createTransaction = createTransaction;
    }

    /**
     * O dono vem do cabeçalho X-User-Id apenas ENQUANTO a autenticação não existe.
     * Nas tarefas DBJ-58 e DBJ-74 ele passa a vir do usuário autenticado.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@RequestHeader("X-User-Id") Long userId,
                                      @Valid @RequestBody TransactionRequest request) {
        return TransactionResponse.from(createTransaction.execute(new CreateTransactionCommand(
                userId, request.type(), request.amount(), request.description(),
                request.category(), request.destinationUserId())));
    }
}