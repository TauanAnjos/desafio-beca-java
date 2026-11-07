package br.com.tauan.ms2_transacoes.application.service;

import br.com.tauan.ms2_transacoes.application.port.in.CreateTransactionCommand;
import br.com.tauan.ms2_transacoes.application.port.out.AccountPort;
import br.com.tauan.ms2_transacoes.application.port.out.TransactionRepositoryPort;
import br.com.tauan.ms2_transacoes.domain.exception.AccountServiceUnavailableException;
import br.com.tauan.ms2_transacoes.domain.exception.InvalidTransactionException;
import br.com.tauan.ms2_transacoes.domain.model.AccountInfo;
import br.com.tauan.ms2_transacoes.domain.model.Category;
import br.com.tauan.ms2_transacoes.domain.model.Transaction;
import br.com.tauan.ms2_transacoes.domain.model.TransactionPolicy;
import br.com.tauan.ms2_transacoes.domain.model.TransactionStatus;
import br.com.tauan.ms2_transacoes.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransactionServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-05T13:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 13, 0);
    private static final LocalDateTime DAY_START = LocalDateTime.of(2026, 11, 5, 0, 0);
    private static final LocalDateTime DAY_END = LocalDateTime.of(2026, 11, 6, 0, 0);

    @Mock
    private TransactionRepositoryPort repository;

    @Mock
    private AccountPort accountPort;

    private CreateTransactionService service;

    @BeforeEach
    void setUp() {
        service = new CreateTransactionService(repository, accountPort, new TransactionPolicy(), CLOCK);
    }

    private CreateTransactionCommand command(TransactionType type, String amount, Category category,
                                             Long destinationUserId) {
        return new CreateTransactionCommand(1L, type, new BigDecimal(amount), "Descricao", category,
                destinationUserId);
    }

    private AccountInfo account(String balance, String dailyLimit, String perTransactionLimit) {
        return new AccountInfo(1L, new BigDecimal(balance), new BigDecimal(dailyLimit),
                new BigDecimal(perTransactionLimit));
    }

    private void saveReturnsTheSameTransaction() {
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deposit_isApprovedWithoutConsultingTheAccount() {
        saveReturnsTheSameTransaction();

        Transaction result = service.execute(command(TransactionType.DEPOSIT, "100", null, null));

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.getOccurredAt()).isEqualTo(NOW);
        verifyNoInteractions(accountPort);
        verify(repository, never()).sumApprovedExpenses(any(), any(), any());
    }

    @Test
    void withdrawal_withinBalanceAndLimits_isApprovedAndSaved() {
        when(accountPort.findByUserId(1L)).thenReturn(Optional.of(account("1000", "3000", "500")));
        when(repository.sumApprovedExpenses(1L, DAY_START, DAY_END)).thenReturn(new BigDecimal("100"));
        saveReturnsTheSameTransaction();

        Transaction result = service.execute(command(TransactionType.WITHDRAWAL, "200", null, null));

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    void withdrawal_greaterThanTheBalance_isSavedAsRejectedWithTheReason() {
        when(accountPort.findByUserId(1L)).thenReturn(Optional.of(account("100", "3000", "500")));
        when(repository.sumApprovedExpenses(1L, DAY_START, DAY_END)).thenReturn(BigDecimal.ZERO);
        saveReturnsTheSameTransaction();

        Transaction result = service.execute(command(TransactionType.WITHDRAWAL, "200", null, null));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("insufficient balance");
    }

    @Test
    void purchase_thatPassesTheDailyLimit_usesTheSpentOfTheCurrentDay() {
        when(accountPort.findByUserId(1L)).thenReturn(Optional.of(account("10000", "3000", "500")));
        when(repository.sumApprovedExpenses(1L, DAY_START, DAY_END)).thenReturn(new BigDecimal("2600"));
        saveReturnsTheSameTransaction();

        Transaction result = service.execute(command(TransactionType.PURCHASE, "500", Category.FOOD, null));

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("daily limit exceeded");
        assertThat(result.getCategory()).isEqualTo(Category.FOOD);
    }

    @Test
    void withdrawal_afterSpendingMostOfTheBalanceToday_isRejectedForBalance() {
        when(accountPort.findByUserId(1L)).thenReturn(Optional.of(account("1000", "3000", "1000")));
        when(repository.sumApprovedExpenses(1L, DAY_START, DAY_END)).thenReturn(new BigDecimal("900"));
        saveReturnsTheSameTransaction();

        Transaction result = service.execute(command(TransactionType.WITHDRAWAL, "900", null, null));

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("insufficient balance");
    }

    @Test
    void expense_withoutAccount_isRejectedAndTheSpentIsNotQueried() {
        when(accountPort.findByUserId(1L)).thenReturn(Optional.empty());
        saveReturnsTheSameTransaction();

        Transaction result = service.execute(command(TransactionType.TRANSFER, "50", null, 2L));

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("account not found for the user");
        verify(repository, never()).sumApprovedExpenses(any(), any(), any());
    }

    @Test
    void invalidRequest_failsBeforeCallingAnyExternalService() {
        assertThatThrownBy(() -> service.execute(command(TransactionType.TRANSFER, "50", null, null)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessage("destination user is required for transfers");

        verifyNoInteractions(accountPort, repository);
    }

    @Test
    void whenTheAccountServiceIsDown_nothingIsSavedAndTheErrorPropagates() {
        when(accountPort.findByUserId(1L)).thenThrow(new AccountServiceUnavailableException("down"));

        assertThatThrownBy(() -> service.execute(command(TransactionType.WITHDRAWAL, "50", null, null)))
                .isInstanceOf(AccountServiceUnavailableException.class);

        verify(repository, never()).save(any(Transaction.class));
    }
}