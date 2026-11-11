package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera o CSV para o Excel em português: UTF-8 com BOM (acentos corretos), separador ponto e vírgula,
 * vírgula decimal e quebra de linha CRLF.
 */
final class TransactionCsvWriter {

    static final String HEADER = String.join(";", "Data", "Tipo", "Categoria", "Descrição",
            "Valor (R$)", "Status", "Motivo da rejeição", "Valor em USD", "Valor em EUR");

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String SEPARATOR = ";";
    private static final String NEW_LINE = "\r\n";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private TransactionCsvWriter() {
    }

    static byte[] write(List<Transaction> transactions) {
        StringBuilder csv = new StringBuilder(HEADER).append(NEW_LINE);
        for (Transaction transaction : transactions) {
            csv.append(String.join(SEPARATOR,
                            transaction.getOccurredAt().format(DATE_TIME),
                            typeLabel(transaction.getType()),
                            categoryLabel(transaction.getCategory()),
                            text(transaction.getDescription()),
                            money(transaction.getAmount()),
                            statusLabel(transaction.getStatus()),
                            text(reasonLabel(transaction.getRejectionReason())),
                            converted(transaction, "USD"),
                            converted(transaction, "EUR")))
                    .append(NEW_LINE);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(BOM);
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static String converted(Transaction transaction, String currency) {
        return transaction.getExchangeRates().stream()
                .filter(rate -> rate.currency().equals(currency))
                .findFirst()
                .map((ExchangeRate rate) -> money(rate.convertFromBrl(transaction.getAmount())))
                .orElse("");
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /**
     * Texto livre: protege contra injeção de fórmula (=, +, -, @) e aplica as aspas do CSV quando preciso.
     */
    private static String text(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = value;
        char first = safe.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            safe = "'" + safe;
        }
        if (safe.contains(";") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static String typeLabel(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> "Depósito";
            case WITHDRAWAL -> "Retirada";
            case TRANSFER -> "Transferência";
            case PURCHASE -> "Compra";
        };
    }

    private static String statusLabel(TransactionStatus status) {
        return switch (status) {
            case APPROVED -> "Aprovada";
            case REJECTED -> "Rejeitada";
        };
    }

    private static String categoryLabel(Category category) {
        return switch (category) {
            case FOOD -> "Alimentação";
            case TRANSPORT -> "Transporte";
            case HOUSING -> "Moradia";
            case HEALTH -> "Saúde";
            case EDUCATION -> "Educação";
            case LEISURE -> "Lazer";
            case SHOPPING -> "Compras";
            case OTHER -> "Outros";
        };
    }

    private static String reasonLabel(String reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason) {
            case "insufficient balance" -> "Saldo insuficiente";
            case "amount exceeds the per-transaction limit" -> "Valor acima do limite por transação";
            case "daily limit exceeded" -> "Limite diário excedido";
            case "account not found for the user" -> "Conta não encontrada";
            default -> reason;
        };
    }
}