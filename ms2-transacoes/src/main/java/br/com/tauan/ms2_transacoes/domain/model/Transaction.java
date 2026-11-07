package br.com.tauan.ms2_transacoes.domain.model;

import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final Long id;
    private final Long userId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String description;
    private final Category category;
    private final Long destinationUserId;
    private final TransactionStatus status;
    private final String rejectionReason;
    private final LocalDateTime occurredAt;

    private Transaction(Long id, Long userId, TransactionType type, BigDecimal amount, String description,
                        Category category, Long destinationUserId, TransactionStatus status,
                        String rejectionReason, LocalDateTime occurredAt) {
        if (userId == null) {
            throw new InvalidTransactionException("user is required");
        }
        if (type == null) {
            throw new InvalidTransactionException("type is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException("amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidTransactionException("amount must have at most 2 decimal places");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidTransactionException("description is required");
        }
        if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidTransactionException(
                    "description must have at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (occurredAt == null) {
            throw new InvalidTransactionException("date and time are required");
        }
        if (type == TransactionType.TRANSFER) {
            if (destinationUserId == null) {
                throw new InvalidTransactionException("destination user is required for transfers");
            }
            if (destinationUserId.equals(userId)) {
                throw new InvalidTransactionException("destination user must be different from the owner");
            }
        } else if (destinationUserId != null) {
            throw new InvalidTransactionException("destination user is only allowed for transfers");
        }
        if (type == TransactionType.PURCHASE && category == null) {
            throw new InvalidTransactionException("category is required for purchases");
        }
        if (status == TransactionStatus.REJECTED) {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new InvalidTransactionException("rejection reason is required");
            }
        } else if (rejectionReason != null) {
            throw new InvalidTransactionException("rejection reason is only allowed for rejected transactions");
        }

        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount.setScale(2);
        this.description = description.trim();
        this.category = category == null ? Category.OTHER : category;
        this.destinationUserId = destinationUserId;
        this.status = status;
        this.rejectionReason = rejectionReason == null ? null : rejectionReason.trim();
        this.occurredAt = occurredAt;
    }

    public static Transaction approved(Long userId, TransactionType type, BigDecimal amount, String description,
                                       Category category, Long destinationUserId, LocalDateTime occurredAt) {
        return new Transaction(null, userId, type, amount, description, category, destinationUserId,
                TransactionStatus.APPROVED, null, occurredAt);
    }

    public static Transaction rejected(Long userId, TransactionType type, BigDecimal amount, String description,
                                       Category category, Long destinationUserId, LocalDateTime occurredAt,
                                       String rejectionReason) {
        return new Transaction(null, userId, type, amount, description, category, destinationUserId,
                TransactionStatus.REJECTED, rejectionReason, occurredAt);
    }

    public static Transaction restore(Long id, Long userId, TransactionType type, BigDecimal amount,
                                      String description, Category category, Long destinationUserId,
                                      TransactionStatus status, String rejectionReason,
                                      LocalDateTime occurredAt) {
        return new Transaction(id, userId, type, amount, description, category, destinationUserId,
                status, rejectionReason, occurredAt);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Long getDestinationUserId() { return destinationUserId; }
    public TransactionStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}