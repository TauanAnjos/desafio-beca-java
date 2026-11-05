package br.com.tauan.ms2_transacoes.domain.model;

public record TransactionDecision(boolean approved, String reason) {

    public static TransactionDecision approve() {
        return new TransactionDecision(true, null);
    }

    public static TransactionDecision reject(String reason) {
        return new TransactionDecision(false, reason);
    }
}