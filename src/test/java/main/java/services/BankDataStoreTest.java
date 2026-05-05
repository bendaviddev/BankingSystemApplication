package main.java.services;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import main.java.enums.BankAccountStatus;
import main.java.enums.BankAccountType;
import main.java.enums.TransactionType;
import main.java.enums.UserRole;
import main.java.models.Transaction;
import main.java.models.User;
import main.java.models.UserBankAccount;

class BankDataStoreTest {

    private User user;

    @BeforeEach
    void setUp() {
        String unique = String.valueOf(System.currentTimeMillis());

        user = BankDataStore.registerUser(
                "ben" + unique,
                "Password123!",
                "Ben",
                "David",
                "123-456-7890",
                "ben" + unique + "@email.com",
                "Oxford, MS"
        );

        assertNotNull(user);
    }

    @Test
    void registerUserCreatesNewUser() {
        assertNotNull(user);
        assertTrue(user.getUsername().startsWith("ben"));
        assertEquals("Ben David", user.getFullName());
        assertNotEquals("Password123!", user.getPassword());
    }

    @Test
    void registerUserPreventsDuplicateUsername() {
        User duplicateUser = BankDataStore.registerUser(
                user.getUsername(),
                "AnotherPassword123!",
                "Other",
                "User",
                "000-000-0000",
                "other" + System.currentTimeMillis() + "@email.com",
                "Somewhere"
        );

        assertNull(duplicateUser);
    }

    @Test
    void registerUserRejectsWeakPassword() {
        User weakPasswordUser = BankDataStore.registerUser(
                "weak" + System.currentTimeMillis(),
                "password",
                "Weak",
                "User",
                "111-111-1111",
                "weak" + System.currentTimeMillis() + "@email.com",
                "Oxford, MS"
        );

        assertNull(weakPasswordUser);
    }

    @Test
    void loginUserWorksWithCorrectPassword() {
        User loggedInUser = BankDataStore.loginUser(user.getUsername(), "Password123!");

        assertNotNull(loggedInUser);
        assertEquals(user.getId(), loggedInUser.getId());
        assertEquals(user.getUsername(), loggedInUser.getUsername());
    }

    @Test
    void loginUserRejectsWrongPassword() {
        User loggedInUser = BankDataStore.loginUser(user.getUsername(), "WrongPassword123!");

        assertNull(loggedInUser);
    }

    @Test
    void openAccountCreatesNewAccount() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        assertNotNull(account);
        assertTrue(account.getAccountId() > 0);
        assertEquals(user.getId(), account.getUserId());
        assertEquals(BankAccountType.BASIC, account.getAccountType());
        assertEquals(100.00, account.getBalance(), 0.001);
        assertEquals(BankAccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    void openAccountRejectsNegativeOpeningBalance() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                -50.00
        );

        assertNull(account);
    }

    @Test
    void getAccountsByUserIdReturnsUserAccounts() {
        UserBankAccount accountOne = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        UserBankAccount accountTwo = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.SAVING,
                200.00
        );

        ArrayList<UserBankAccount> accounts = BankDataStore.getAccountsByUserId(user.getId());

        assertTrue(accounts.size() >= 2);
        assertTrue(accounts.stream().anyMatch(account -> account.getAccountId() == accountOne.getAccountId()));
        assertTrue(accounts.stream().anyMatch(account -> account.getAccountId() == accountTwo.getAccountId()));
    }

    @Test
    void depositAddsMoneyToAccount() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        boolean success = BankDataStore.deposit(
                user.getId(),
                account.getAccountId(),
                50.00
        );

        UserBankAccount updatedAccount = BankDataStore.getAccountById(account.getAccountId());

        assertTrue(success);
        assertNotNull(updatedAccount);
        assertEquals(150.00, updatedAccount.getBalance(), 0.001);
    }

    @Test
    void depositRejectsNegativeAmount() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        boolean success = BankDataStore.deposit(
                user.getId(),
                account.getAccountId(),
                -25.00
        );

        UserBankAccount updatedAccount = BankDataStore.getAccountById(account.getAccountId());

        assertFalse(success);
        assertEquals(100.00, updatedAccount.getBalance(), 0.001);
    }

    @Test
    void withdrawSubtractsMoneyFromAccount() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        boolean success = BankDataStore.withdraw(
                user.getId(),
                account.getAccountId(),
                40.00
        );

        UserBankAccount updatedAccount = BankDataStore.getAccountById(account.getAccountId());

        assertTrue(success);
        assertEquals(60.00, updatedAccount.getBalance(), 0.001);
    }

    @Test
    void withdrawPreventsOverdraft() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        boolean success = BankDataStore.withdraw(
                user.getId(),
                account.getAccountId(),
                150.00
        );

        UserBankAccount updatedAccount = BankDataStore.getAccountById(account.getAccountId());

        assertFalse(success);
        assertEquals(100.00, updatedAccount.getBalance(), 0.001);
    }

    @Test
    void transferMovesMoneyBetweenAccounts() {
        UserBankAccount checking = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                500.00
        );

        UserBankAccount savings = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.SAVING,
                100.00
        );

        boolean success = BankDataStore.transfer(
                user.getId(),
                checking.getAccountId(),
                savings.getAccountId(),
                150.00
        );

        UserBankAccount updatedChecking = BankDataStore.getAccountById(checking.getAccountId());
        UserBankAccount updatedSavings = BankDataStore.getAccountById(savings.getAccountId());

        assertTrue(success);
        assertEquals(350.00, updatedChecking.getBalance(), 0.001);
        assertEquals(250.00, updatedSavings.getBalance(), 0.001);
    }

    @Test
    void transferPreventsOverdraft() {
        UserBankAccount checking = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        UserBankAccount savings = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.SAVING,
                100.00
        );

        boolean success = BankDataStore.transfer(
                user.getId(),
                checking.getAccountId(),
                savings.getAccountId(),
                300.00
        );

        UserBankAccount updatedChecking = BankDataStore.getAccountById(checking.getAccountId());
        UserBankAccount updatedSavings = BankDataStore.getAccountById(savings.getAccountId());

        assertFalse(success);
        assertEquals(100.00, updatedChecking.getBalance(), 0.001);
        assertEquals(100.00, updatedSavings.getBalance(), 0.001);
    }

    @Test
    void frozenAccountCannotDepositWithdrawOrTransfer() {
        UserBankAccount checking = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                500.00
        );

        UserBankAccount savings = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.SAVING,
                500.00
        );

        BankDataStore.updateAccountStatus(
                checking.getAccountId(),
                BankAccountStatus.FROZEN
        );

        assertFalse(BankDataStore.deposit(user.getId(), checking.getAccountId(), 100.00));
        assertFalse(BankDataStore.withdraw(user.getId(), checking.getAccountId(), 100.00));
        assertFalse(BankDataStore.transfer(user.getId(), checking.getAccountId(), savings.getAccountId(), 100.00));

        UserBankAccount updatedChecking = BankDataStore.getAccountById(checking.getAccountId());
        UserBankAccount updatedSavings = BankDataStore.getAccountById(savings.getAccountId());

        assertEquals(BankAccountStatus.FROZEN, updatedChecking.getStatus());
        assertEquals(500.00, updatedChecking.getBalance(), 0.001);
        assertEquals(500.00, updatedSavings.getBalance(), 0.001);
    }

    @Test
    void closedAccountCannotDepositWithdrawOrTransfer() {
        UserBankAccount checking = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                500.00
        );

        UserBankAccount savings = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.SAVING,
                500.00
        );

        BankDataStore.updateAccountStatus(
                checking.getAccountId(),
                BankAccountStatus.CLOSED
        );

        assertFalse(BankDataStore.deposit(user.getId(), checking.getAccountId(), 100.00));
        assertFalse(BankDataStore.withdraw(user.getId(), checking.getAccountId(), 100.00));
        assertFalse(BankDataStore.transfer(user.getId(), checking.getAccountId(), savings.getAccountId(), 100.00));

        UserBankAccount updatedChecking = BankDataStore.getAccountById(checking.getAccountId());

        assertEquals(BankAccountStatus.CLOSED, updatedChecking.getStatus());
        assertEquals(500.00, updatedChecking.getBalance(), 0.001);
    }

    @Test
    void getTransactionsForUserReturnsTransactionHistory() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        BankDataStore.deposit(user.getId(), account.getAccountId(), 25.00);
        BankDataStore.withdraw(user.getId(), account.getAccountId(), 10.00);

        ArrayList<Transaction> transactions = BankDataStore.getTransactionsForUser(user.getId());

        assertFalse(transactions.isEmpty());
        assertTrue(transactions.stream().anyMatch(transaction -> transaction.getTransactionType() == TransactionType.DEPOSIT));
        assertTrue(transactions.stream().anyMatch(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL));
    }

    @Test
    void filterTransactionsByTypeReturnsOnlyMatchingTransactions() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        BankDataStore.deposit(user.getId(), account.getAccountId(), 25.00);
        BankDataStore.withdraw(user.getId(), account.getAccountId(), 10.00);

        ArrayList<Transaction> deposits
                = BankDataStore.getTransactionsForUserByType(user.getId(), TransactionType.DEPOSIT);

        assertFalse(deposits.isEmpty());

        for (Transaction transaction : deposits) {
            assertEquals(TransactionType.DEPOSIT, transaction.getTransactionType());
        }
    }

    @Test
    void getTransactionsForUserByAccountReturnsOnlyThatAccountsTransactions() {
        UserBankAccount checking = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        UserBankAccount savings = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.SAVING,
                200.00
        );

        BankDataStore.deposit(user.getId(), checking.getAccountId(), 25.00);
        BankDataStore.deposit(user.getId(), savings.getAccountId(), 50.00);

        ArrayList<Transaction> checkingTransactions
                = BankDataStore.getTransactionsForUserByAccount(user.getId(), checking.getAccountId());

        assertFalse(checkingTransactions.isEmpty());

        for (Transaction transaction : checkingTransactions) {
            assertEquals(checking.getAccountId(), transaction.getAccountId());
        }
    }

    @Test
    void latestTransactionForAccountReturnsMostRecentTransaction() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        BankDataStore.deposit(user.getId(), account.getAccountId(), 25.00);

        Transaction latestTransaction = BankDataStore.getLatestTransactionForAccount(account.getAccountId());

        assertNotNull(latestTransaction);
        assertEquals(TransactionType.DEPOSIT, latestTransaction.getTransactionType());
        assertEquals(25.00, latestTransaction.getAmount(), 0.001);
    }

    @Test
    void adminCanChangeAccountStatus() {
        UserBankAccount account = BankDataStore.openAccount(
                user.getId(),
                BankAccountType.BASIC,
                100.00
        );

        boolean success = BankDataStore.updateAccountStatus(
                account.getAccountId(),
                BankAccountStatus.FROZEN
        );

        UserBankAccount updatedAccount = BankDataStore.getAccountById(account.getAccountId());

        assertTrue(success);
        assertEquals(BankAccountStatus.FROZEN, updatedAccount.getStatus());
    }

    @Test
    void updateAccountStatusReturnsFalseForInvalidAccount() {
        boolean success = BankDataStore.updateAccountStatus(
                -999,
                BankAccountStatus.FROZEN
        );

        assertFalse(success);
    }

    @Test
    void registeredUserHasUserRoleByDefault() {
        assertEquals(UserRole.USER, user.getRole());
        assertFalse(user.isAdmin());
    }

    @Test
    void adminUserHasAdminRole() {
        User admin = new User(
                0,
                "adminTest" + System.currentTimeMillis(),
                "hashedPassword",
                "Admin",
                "User",
                "123-456-7890",
                "admin" + System.currentTimeMillis() + "@email.com",
                "Oxford, MS",
                UserRole.ADMIN
        );

        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(admin.isAdmin());
    }
}
