package br.com.tauan.ms2_transacoes.domain.model;

import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;

import java.math.BigDecimal;

/**
 * Regras de aprovação. Java puro: recebe os dados já buscados e devolve a decisão.
 *
 * O saldo da API Mock não é atualizado por este serviço. Por isso o saldo DISPONÍVEL é o saldo
 * informado, mais os depósitos aprovados no dia, menos as despesas aprovadas no dia.
 */
public class TransactionPolicy {

    public TransactionDecision evaluate(TransactionType type, BigDecimal amount,
                                        AccountInfo account, BigDecimal spentToday) {
        return evaluate(type, amount, account, spentToday, null);
    }

    /**
     * @param account       saldo e limites do usuário; nulo se ele não tem conta na API Mock
     * @param spentToday    soma das despesas aprovadas do usuário no dia; nulo conta como zero
     * @param receivedToday soma dos depósitos aprovados do usuário no dia; nulo conta como zero
     */
    public TransactionDecision evaluate(TransactionType type, BigDecimal amount, AccountInfo account,
                                        BigDecimal spentToday, BigDecimal receivedToday) {
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

        BigDecimal alreadySpent = spentToday == null ? BigDecimal.ZERO : spentToday;
        BigDecimal alreadyReceived = receivedToday == null ? BigDecimal.ZERO : receivedToday;
        BigDecimal availableBalance = account.balance().add(alreadyReceived).subtract(alreadySpent);

        if (amount.compareTo(availableBalance) > 0) {
            return TransactionDecision.reject("insufficient balance");
        }
        if (amount.compareTo(account.perTransactionLimit()) > 0) {
            return TransactionDecision.reject("amount exceeds the per-transaction limit");
        }
        if (alreadySpent.add(amount).compareTo(account.dailyLimit()) > 0) {
            return TransactionDecision.reject("daily limit exceeded");
        }
        return TransactionDecision.approve();
    }
}