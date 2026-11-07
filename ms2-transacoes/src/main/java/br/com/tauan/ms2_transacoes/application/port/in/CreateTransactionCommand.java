package br.com.tauan.ms2_transacoes.application.port.in;

import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;

import java.math.BigDecimal;

public record CreateTransactionCommand(Long userId, TransactionType type, BigDecimal amount,
                                       String description, Category category, Long destinationUserId) {
}