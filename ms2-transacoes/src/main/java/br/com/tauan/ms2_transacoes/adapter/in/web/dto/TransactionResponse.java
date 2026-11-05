package br.com.tauan.ms2_transacoes.adapter.in.web.dto;

import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long userId,
        TransactionType type,
        BigDecimal amount,
        String description,
        Category category,
        Long destinationUserId,
        TransactionStatus status,
        String rejectionReason,
        LocalDateTime occurredAt
) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getDestinationUserId(),
                transaction.getStatus(),
                transaction.getRejectionReason(),
                transaction.getOccurredAt());
    }
}