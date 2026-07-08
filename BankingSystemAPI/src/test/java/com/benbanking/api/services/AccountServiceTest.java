package com.benbanking.api.services;

import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.exceptions.AccountNotActiveException;
import com.benbanking.api.exceptions.BadRequestException;
import com.benbanking.api.exceptions.InsufficientFundsException;
import com.benbanking.api.exceptions.NotFoundException;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.models.User;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.repositories.TransactionRepository;
import com.benbanking.api.repositories.TransactionRepository.TransactionFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TransactionRepository transactionRepository;

    private User newUser(String prefix) {
        String username = prefix + "_" + System.nanoTime();
        return authService.registerUser(username, "Demo123!", "Test", "User", "555-0100",
                username + "@example.com", "1 Test St");
    }

    @Test
    void depositAndWithdrawHappyPathTracksRunningBalance() {
        User user = newUser("acct_happy");
        UserBankAccount account = accountService.openAccount(user.getId(), BankAccountType.CHECKING, new BigDecimal("500.00"));

        Transaction deposit = accountService.deposit(user.getId(), account.getAccountId(), new BigDecimal("200.00"), TransactionCategory.INCOME, null);
        assertEquals(new BigDecimal("700.00"), deposit.getRunningBalance());
        assertEquals(TransactionStatus.COMPLETED, deposit.getStatus());
        assertEquals(TransactionType.DEPOSIT, deposit.getTransactionType());
        assertNotNull(deposit.getReference());

        Transaction withdrawal = accountService.withdraw(user.getId(), account.getAccountId(), new BigDecimal("100.00"), TransactionCategory.OTHER, null);
        assertEquals(new BigDecimal("600.00"), withdrawal.getRunningBalance());
        assertEquals(TransactionStatus.COMPLETED, withdrawal.getStatus());

        UserBankAccount reloaded = accountService.getAccountForUser(account.getAccountId(), user.getId());
        assertEquals(0, new BigDecimal("600.00").compareTo(reloaded.getBalance()));
    }

    @Test
    void transferHappyPathWritesBothLegs() {
        User user = newUser("acct_transfer");
        UserBankAccount from = accountService.openAccount(user.getId(), BankAccountType.CHECKING, new BigDecimal("300.00"));
        UserBankAccount to = accountService.openAccount(user.getId(), BankAccountType.SAVINGS, new BigDecimal("50.00"));

        AccountService.TransferResult result = accountService.transferInternal(user.getId(), from.getAccountId(), to.getAccountId(), new BigDecimal("120.00"), "moving cash");

        assertEquals(TransactionType.TRANSFER_OUT, result.outLeg().getTransactionType());
        assertEquals(TransactionType.TRANSFER_IN, result.inLeg().getTransactionType());
        assertEquals(new BigDecimal("180.00"), result.outLeg().getRunningBalance());
        assertEquals(new BigDecimal("170.00"), result.inLeg().getRunningBalance());
        assertEquals(to.getAccountId(), result.outLeg().getCounterpartyAccountId());
        assertEquals(from.getAccountId(), result.inLeg().getCounterpartyAccountId());
        assertNotEquals(result.outLeg().getReference(), result.inLeg().getReference());
    }

    @Test
    void insufficientFundsRecordsFailedRowAndThrows() {
        User user = newUser("acct_insufficient");
        UserBankAccount account = accountService.openAccount(user.getId(), BankAccountType.CHECKING, new BigDecimal("50.00"));

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
                accountService.withdraw(user.getId(), account.getAccountId(), new BigDecimal("500.00"), TransactionCategory.OTHER, null));
        assertTrue(ex.getMessage().contains("50.00"));

        List<Transaction> rows = transactionRepository.findFiltered(new TransactionFilter(
                user.getId(), account.getAccountId(), null, TransactionStatus.FAILED, null, null, null, null, null, 10, 0));
        assertEquals(1, rows.size());
        assertNull(rows.get(0).getRunningBalance());
        assertEquals(new BigDecimal("500.00"), rows.get(0).getAmount());

        // Balance must be untouched by the failed attempt.
        UserBankAccount reloaded = accountService.getAccountForUser(account.getAccountId(), user.getId());
        assertEquals(0, new BigDecimal("50.00").compareTo(reloaded.getBalance()));
    }

    @Test
    void frozenAccountRejectsDeposit() {
        User user = newUser("acct_frozen");
        User admin = newUser("acct_admin");
        UserBankAccount account = accountService.openAccount(user.getId(), BankAccountType.CHECKING, new BigDecimal("100.00"));

        accountService.updateAccountStatus(admin.getId(), account.getAccountId(), BankAccountStatus.FROZEN);

        assertThrows(AccountNotActiveException.class, () ->
                accountService.deposit(user.getId(), account.getAccountId(), new BigDecimal("10.00"), TransactionCategory.INCOME, null));
    }

    @Test
    void invalidAmountIsRejected() {
        User user = newUser("acct_invalid_amount");
        UserBankAccount account = accountService.openAccount(user.getId(), BankAccountType.CHECKING, new BigDecimal("100.00"));

        assertThrows(BadRequestException.class, () ->
                accountService.deposit(user.getId(), account.getAccountId(), BigDecimal.ZERO, TransactionCategory.INCOME, null));
        assertThrows(BadRequestException.class, () ->
                accountService.withdraw(user.getId(), account.getAccountId(), new BigDecimal("-5.00"), TransactionCategory.OTHER, null));
    }

    @Test
    void ownershipViolationOnDepositThrowsNotFound() {
        User owner = newUser("acct_owner");
        User attacker = newUser("acct_attacker");
        UserBankAccount account = accountService.openAccount(owner.getId(), BankAccountType.CHECKING, new BigDecimal("100.00"));

        assertThrows(NotFoundException.class, () ->
                accountService.deposit(attacker.getId(), account.getAccountId(), new BigDecimal("10.00"), TransactionCategory.INCOME, null));
    }

    @Test
    void internalTransferDestinationMustBeCallerOwned() {
        User owner = newUser("acct_transfer_owner");
        User other = newUser("acct_transfer_other");
        UserBankAccount ownerAccount = accountService.openAccount(owner.getId(), BankAccountType.CHECKING, new BigDecimal("200.00"));
        UserBankAccount otherAccount = accountService.openAccount(other.getId(), BankAccountType.CHECKING, new BigDecimal("50.00"));

        assertThrows(NotFoundException.class, () ->
                accountService.transferInternal(owner.getId(), ownerAccount.getAccountId(), otherAccount.getAccountId(), new BigDecimal("10.00"), null));

        // Balances must be unaffected by the rejected cross-user transfer attempt.
        UserBankAccount reloadedOther = accountService.getAccountForUser(otherAccount.getAccountId(), other.getId());
        assertEquals(0, new BigDecimal("50.00").compareTo(reloadedOther.getBalance()));
    }
}
