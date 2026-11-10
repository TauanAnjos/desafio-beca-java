package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.adapter.in.web.dto.TransactionRequest;
import br.com.tauan.ms2_transacoes.adapter.in.web.dto.TransactionResponse;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionCommand;
import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionUseCase;
import br.com.tauan.ms2_transacoes.domain.model.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * O dono da transação é sempre o usuário autenticado: o corpo da requisição não traz usuário.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(Authentication authentication,
                                      @Valid @RequestBody TransactionRequest request) {
        AuthenticatedUser user = CurrentUser.from(authentication);
        return TransactionResponse.from(createTransaction.execute(new CreateTransactionCommand(
                user.id(), request.type(), request.amount(), request.description(),
                request.category(), request.destinationUserId())));
    }
}