package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.ListTransactionsQuery;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import br.com.tauan.ms2_transacoes.domain.exception.TransactionNotFoundException;
import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryTransactionsServiceTest {

    @Mock
    private TransactionRepositoryPort repository;

    @InjectMocks
    private QueryTransactionsService service;

    private Transaction transactionOf(Long ownerId) {
        return Transaction.restore(10L, ownerId, TransactionType.DEPOSIT, new BigDecimal("100.00"), "Pix",
                Category.OTHER, null, TransactionStatus.APPROVED, null, LocalDateTime.of(2026, 11, 5, 10, 0));
    }

    @Test
    void get_returnsTheTransactionOfTheOwner() {
        Transaction transaction = transactionOf(1L);
        when(repository.findById(10L)).thenReturn(Optional.of(transaction));

        assertThat(service.execute(1L, 10L)).isSameAs(transaction);
    }

    @Test
    void get_whenItDoesNotExist_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, 99L)).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void get_ofAnotherUser_isReportedAsNotFound() {
        when(repository.findById(10L)).thenReturn(Optional.of(transactionOf(2L)));

        assertThatThrownBy(() -> service.execute(1L, 10L)).isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void list_convertsTheDatesAndIncludesTheWholeLastDay() {
        List<Transaction> expected = List.of(transactionOf(1L));
        when(repository.findByUser(1L, LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 6, 0, 0), TransactionType.PURCHASE)).thenReturn(expected);

        List<Transaction> result = service.execute(new ListTransactionsQuery(1L,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), TransactionType.PURCHASE));

        assertThat(result).isSameAs(expected);
    }

    @Test
    void list_withoutFilters_passesNullsToTheRepository() {
        List<Transaction> expected = List.of(transactionOf(1L));
        when(repository.findByUser(1L, null, null, null)).thenReturn(expected);

        assertThat(service.execute(new ListTransactionsQuery(1L, null, null, null))).isSameAs(expected);
    }

    @Test
    void list_withFromAfterTo_isInvalid() {
        assertThatThrownBy(() -> service.execute(new ListTransactionsQuery(1L,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1), null)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("'from' must not be after 'to'");

        verifyNoInteractions(repository);
    }
}