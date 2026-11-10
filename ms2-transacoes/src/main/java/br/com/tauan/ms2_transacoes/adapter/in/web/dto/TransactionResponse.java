package br.com.tauan.ms2_transacoes.adapter.in.web.dto;

import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime occurredAt,
        boolean conversionAvailable,
        List<Conversion> conversions
) {

    /**
     * O valor "amount" é o original, em reais. A taxa é de reais por unidade da moeda.
     */
    public record Conversion(String currency, BigDecimal rate, LocalDate rateDate, BigDecimal convertedAmount) {
    }

    public static TransactionResponse from(Transaction transaction) {
        List<Conversion> conversions = transaction.getExchangeRates().stream()
                .map(rate -> new Conversion(rate.currency(), rate.rate(), rate.rateDate(),
                        rate.convertFromBrl(transaction.getAmount())))
                .toList();
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
                transaction.getOccurredAt(),
                !conversions.isEmpty(),
                conversions);
    }
}