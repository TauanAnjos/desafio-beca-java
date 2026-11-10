package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.adapter.in.web.dto.BalanceResponse;
import br.com.tauan.ms2_transacoes.application.port.in.GetBalanceUseCase;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/balance")
public class BalanceController {

    private final GetBalanceUseCase getBalance;

    public BalanceController(GetBalanceUseCase getBalance) {
        this.getBalance = getBalance;
    }

    @GetMapping
    public BalanceResponse get(Authentication authentication) {
        return BalanceResponse.from(getBalance.execute(CurrentUser.from(authentication).id()));
    }
}