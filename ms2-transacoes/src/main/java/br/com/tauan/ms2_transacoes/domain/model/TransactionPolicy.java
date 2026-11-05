package br.com.tauan.ms2_transacoes.domain.model;

import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;

import java.math.BigDecimal;

public class TransactionPolicy {

    /**
     * @param account    saldo e limites do usuário; nulo se ele não tem conta na API Mock
     * @param spentToday soma das despesas aprovadas do usuário no dia; nulo conta como zero
     */
    public TransactionDecision evaluate(TransactionType type, BigDecimal amount,
                                        AccountInfo account, BigDecimal spentToday) {
        if (type == null) {
            throw new InvalidTransactionException("type is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException("amount must be greater than zero");
        }

        if (!type.isExpense()) {
            return TransactionDecision.approve();
        }
        if (account == null) {
            return TransactionDecision.reject("account not found for the user");
        }
        if (amount.compareTo(account.balance()) > 0) {
            return TransactionDecision.reject("insufficient balance");
        }
        if (amount.compareTo(account.perTransactionLimit()) > 0) {
            return TransactionDecision.reject("amount exceeds the per-transaction limit");
        }
        BigDecimal alreadySpent = spentToday == null ? BigDecimal.ZERO : spentToday;
        if (alreadySpent.add(amount).compareTo(account.dailyLimit()) > 0) {
            return TransactionDecision.reject("daily limit exceeded");
        }
        return TransactionDecision.approve();
    }
}