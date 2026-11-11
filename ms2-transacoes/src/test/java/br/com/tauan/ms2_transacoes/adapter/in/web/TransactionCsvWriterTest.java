package br.com.tauan.ms2_transacoes.adapter.in.web;

import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.ExchangeRate;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCsvWriterTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 10, 10, 0);

    private String csv(List<Transaction> transactions) {
        byte[] bytes = TransactionCsvWriter.write(transactions);
        return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
    }

    private Transaction purchase(String amount, String description, List<ExchangeRate> rates) {
        return Transaction.restore(null, 1L, TransactionType.PURCHASE, new BigDecimal(amount), description,
                Category.FOOD, null, TransactionStatus.APPROVED, null, WHEN, rates);
    }

    @Test
    void startsWithTheUtf8Bom_soTheExcelKeepsTheAccents() {
        byte[] bytes = TransactionCsvWriter.write(List.of());

        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);
    }

    @Test
    void withoutTransactions_onlyTheHeaderIsWritten_withSemicolonsAndCrLf() {
        assertThat(csv(List.of())).isEqualTo(
                "Data;Tipo;Categoria;Descrição;Valor (R$);Status;Motivo da rejeição;Valor em USD;Valor em EUR\r\n");
    }

    @Test
    void anApprovedPurchase_isWrittenInPortuguese_withCommaDecimalAndEmptyRateColumns() {
        String line = csv(List.of(purchase("30.00", "Mercado", List.of()))).split("\r\n")[1];

        assertThat(line).isEqualTo("10/11/2026 10:00:00;Compra;Alimentação;Mercado;30,00;Aprovada;;;");
    }

    @Test
    void aRejectedTransaction_showsTheReasonInPortuguese() {
        Transaction rejected = Transaction.restore(null, 1L, TransactionType.WITHDRAWAL, new BigDecimal("999999.00"),
                "Saque grande", null, null, TransactionStatus.REJECTED, "insufficient balance", WHEN);

        String line = csv(List.of(rejected)).split("\r\n")[1];

        assertThat(line).isEqualTo(
                "10/11/2026 10:00:00;Retirada;Outros;Saque grande;999999,00;Rejeitada;Saldo insuficiente;;");
    }

    @Test
    void theConvertedValuesAreWritten_whenTheTransactionHasRates() {
        List<ExchangeRate> rates = List.of(
                new ExchangeRate("USD", new BigDecimal("5.50"), LocalDate.of(2026, 11, 9)),
                new ExchangeRate("EUR", new BigDecimal("6.00"), LocalDate.of(2026, 11, 9)));

        String line = csv(List.of(purchase("110.00", "Mercado", rates))).split("\r\n")[1];

        assertThat(line).endsWith("Aprovada;;20,00;18,33");
    }

    @Test
    void aDescriptionWithSemicolonAndQuotes_isQuoted() {
        String line = csv(List.of(purchase("10.00", "Mercado; \"centro\"", List.of()))).split("\r\n")[1];

        assertThat(line).contains(";\"Mercado; \"\"centro\"\"\";");
    }

    @Test
    void aDescriptionThatLooksLikeAFormula_isNeutralized() {
        String equals = csv(List.of(purchase("10.00", "=SUM(A1:A9)", List.of()))).split("\r\n")[1];
        String at = csv(List.of(purchase("10.00", "@cmd", List.of()))).split("\r\n")[1];
        String plus = csv(List.of(purchase("10.00", "+1+1", List.of()))).split("\r\n")[1];

        assertThat(equals).contains(";'=SUM(A1:A9);");
        assertThat(at).contains(";'@cmd;");
        assertThat(plus).contains(";'+1+1;");
    }

    @Test
    void accentsInTheDescription_arePreserved() {
        assertThat(csv(List.of(purchase("10.00", "Açaí e pão de queijo", List.of()))))
                .contains("Açaí e pão de queijo");
    }
}