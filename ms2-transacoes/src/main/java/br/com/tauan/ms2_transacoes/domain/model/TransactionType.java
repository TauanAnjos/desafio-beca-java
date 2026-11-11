package br.com.tauan.ms2_transacoes.domain.model;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER,
    PURCHASE;

    /**
     * Saídas de dinheiro: retirada, transferência enviada e compra.
     * Só elas consultam saldo e limites e contam como despesa.
     */
    public boolean isExpense() {
        return this != DEPOSIT;
    }

    /**
     * Entradas de dinheiro que aumentam o saldo disponível do dia.
     */
    public boolean isIncome() {
        return this == DEPOSIT;
    }
}