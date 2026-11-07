package br.com.tauan.ms2_transacoes.adapter.in.web.dto;

import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull TransactionType type,
        @NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 255) String description,
        Category category,
        @Positive Long destinationUserId
) {
}