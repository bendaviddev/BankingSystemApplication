package com.benbanking.api.services;

import com.benbanking.api.dto.AccountLookupResponse;
import com.benbanking.api.enums.ActivityType;
import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;
import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.exceptions.AccountNotActiveException;
import com.benbanking.api.exceptions.BadRequestException;
import com.benbanking.api.exceptions.InsufficientFundsException;
import com.benbanking.api.exceptions.NotFoundException;
import com.benbanking.api.models.Log;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.models.User;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.repositories.AccountRepository;
import com.benbanking.api.repositories.LogRepository;
import com.benbanking.api.repositories.TransactionRepository;
import com.benbanking.api.repositories.UserRepository;
import com.benbanking.api.util.Masking;
import com.benbanking.api.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class AccountService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    private static final int MAX_ACCOUNTS_PER_USER = 5;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LogRepository logRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LogRepository logRepository,
            UserRepository userRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
    }

    // ── Accounts ────────────────────────────────────────────────────────────

    public UserBankAccount openAccount(int userId, BankAccountType accountType, BigDecimal openingBalance) {
        if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Opening balance cannot be negative.");
        }
        if (accountRepository.countByUserId(userId) >= MAX_ACCOUNTS_PER_USER) {
            throw new BadRequestException("You may not open more than " + MAX_ACCOUNTS_PER_USER + " accounts.");
        }

        String accountNumber = "ACCT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        UserBankAccount account = new UserBankAccount(
                0, userId, accountNumber, accountType, Currency.DOLLAR,
                MoneyUtil.normalize(openingBalance), BankAccountStatus.ACTIVE, now, now
        );

        UserBankAccount savedAccount = accountRepository.save(account);
        addLog(userId, ActivityType.OPEN_NEW_BANK_ACCOUNT, "User opened a new " + accountType + " account.");
        return savedAccount;
    }

    public ArrayList<UserBankAccount> getAccountsByUserId(int userId) {
        return accountRepository.findByUserId(userId);
    }

    public UserBankAccount getAccountForUser(int accountId, int userId) {
        UserBankAccount account = accountRepository.findByIdAndUserId(accountId, userId);
        if (account == null) {
            throw new NotFoundException("Account not found.");
        }
        return account;
    }

    public void updateAccountStatus(int adminUserId, int accountId, BankAccountStatus newStatus) {
        UserBankAccount account = accountRepository.findById(accountId);
        if (account == null) {
            throw new NotFoundException("Account not found.");
        }
        accountRepository.updateStatus(accountId, newStatus, LocalDateTime.now());
        addLog(adminUserId, ActivityType.ADMIN_ACCOUNT_STATUS_CHANGE,
                "Admin set account #" + accountId + " (" + account.getAccountNumber() + ") status to " + newStatus + ".");
    }

    public AccountLookupResponse lookupAccount(String accountNumber) {
        UserBankAccount account = accountRepository.findByAccountNumber(accountNumber)
                .filter(a -> a.getStatus() == BankAccountStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Account not found."));

        User owner = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new NotFoundException("Account not found."));

        return new AccountLookupResponse(
                Masking.maskAccountNumber(account.getAccountNumber()),
                owner.getFirstName(),
                Masking.lastInitial(owner.getLastName())
        );
    }

    // ── Money operations ───────────────────────────────────────────────────

    @Transactional(noRollbackFor = InsufficientFundsException.class)
    public Transaction deposit(int userId, int accountId, BigDecimal rawAmount, TransactionCategory category, String memo) {
        BigDecimal amount = requireValidAmount(rawAmount);
        UserBankAccount account = getAccountForUser(accountId, userId);
        TransactionCategory finalCategory = category != null ? category : TransactionCategory.INCOME;
        LocalDateTime now = LocalDateTime.now();

        creditOrFail(accountId, amount, now);
        BigDecimal newBalance = accountRepository.getBalance(accountId);

        Transaction txn = transactionRepository.save(new Transaction(
                0, newReference(), accountId, null, TransactionType.DEPOSIT, TransactionStatus.COMPLETED,
                amount, newBalance, finalCategory, "Deposit to account " + account.getAccountNumber(), memo, now
        ));

        addLog(userId, ActivityType.DEPOSIT, "User deposited $" + amount + ".");
        return txn;
    }

    @Transactional(noRollbackFor = InsufficientFundsException.class)
    public Transaction withdraw(int userId, int accountId, BigDecimal rawAmount, TransactionCategory category, String memo) {
        BigDecimal amount = requireValidAmount(rawAmount);
        UserBankAccount account = getAccountForUser(accountId, userId);
        TransactionCategory finalCategory = category != null ? category : TransactionCategory.OTHER;
        LocalDateTime now = LocalDateTime.now();

        debitOrFail(accountId, amount, now, userId, TransactionType.WITHDRAWAL, finalCategory,
                "Withdrawal from account " + account.getAccountNumber(), memo);
        BigDecimal newBalance = accountRepository.getBalance(accountId);

        Transaction txn = transactionRepository.save(new Transaction(
                0, newReference(), accountId, null, TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED,
                amount, newBalance, finalCategory, "Withdrawal from account " + account.getAccountNumber(), memo, now
        ));

        addLog(userId, ActivityType.WITHDRAWAL, "User withdrew $" + amount + ".");
        return txn;
    }

    /** Internal transfer: both accounts must belong to the caller (fixes the pre-existing ownership bug). */
    @Transactional(noRollbackFor = InsufficientFundsException.class)
    public TransferResult transferInternal(int userId, int fromAccountId, int toAccountId, BigDecimal rawAmount, String memo) {
        BigDecimal amount = requireValidAmount(rawAmount);
        if (fromAccountId == toAccountId) {
            throw new BadRequestException("Cannot transfer to the same account.");
        }

        UserBankAccount fromAccount = getAccountForUser(fromAccountId, userId);
        UserBankAccount toAccount = getAccountForUser(toAccountId, userId);

        LocalDateTime now = LocalDateTime.now();
        applyTransferLegsInLockOrder(fromAccountId, toAccountId, amount, now, userId,
                TransactionCategory.TRANSFER, "Transfer from account " + fromAccount.getAccountNumber());

        BigDecimal fromBalance = accountRepository.getBalance(fromAccountId);
        BigDecimal toBalance = accountRepository.getBalance(toAccountId);

        String outRef = newReference();
        String inRef = newReference();

        Transaction outLeg = transactionRepository.save(new Transaction(
                0, outRef, fromAccountId, toAccountId, TransactionType.TRANSFER_OUT, TransactionStatus.COMPLETED,
                amount, fromBalance, TransactionCategory.TRANSFER,
                "Transfer to account " + toAccount.getAccountNumber(), memo, now
        ));
        Transaction inLeg = transactionRepository.save(new Transaction(
                0, inRef, toAccountId, fromAccountId, TransactionType.TRANSFER_IN, TransactionStatus.COMPLETED,
                amount, toBalance, TransactionCategory.TRANSFER,
                "Transfer from account " + fromAccount.getAccountNumber(), memo, now
        ));

        addLog(userId, ActivityType.TRANSFER, "User transferred $" + amount + ".");
        return new TransferResult(outLeg, inLeg);
    }

    /** External transfer by exact account number; recipient must be a different, ACTIVE account. */
    @Transactional(noRollbackFor = InsufficientFundsException.class)
    public ExternalTransferResult transferExternal(int userId, int fromAccountId, String toAccountNumber, BigDecimal rawAmount, String memo) {
        BigDecimal amount = requireValidAmount(rawAmount);
        UserBankAccount fromAccount = getAccountForUser(fromAccountId, userId);

        UserBankAccount toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new NotFoundException("Recipient account not found."));

        if (toAccount.getUserId() == userId) {
            throw new BadRequestException("Use an internal transfer to move money between your own accounts.");
        }
        if (toAccount.getStatus() != BankAccountStatus.ACTIVE) {
            throw new NotFoundException("Recipient account not found.");
        }

        LocalDateTime now = LocalDateTime.now();
        applyTransferLegsInLockOrder(fromAccountId, toAccount.getAccountId(), amount, now, userId,
                TransactionCategory.TRANSFER, "External transfer from account " + fromAccount.getAccountNumber());

        BigDecimal fromBalance = accountRepository.getBalance(fromAccountId);
        BigDecimal toBalance = accountRepository.getBalance(toAccount.getAccountId());

        String outRef = newReference();
        String inRef = newReference();

        Transaction outLeg = transactionRepository.save(new Transaction(
                0, outRef, fromAccountId, toAccount.getAccountId(), TransactionType.TRANSFER_OUT, TransactionStatus.COMPLETED,
                amount, fromBalance, TransactionCategory.TRANSFER,
                "External transfer to " + toAccountNumber, memo, now
        ));
        Transaction inLeg = transactionRepository.save(new Transaction(
                0, inRef, toAccount.getAccountId(), fromAccountId, TransactionType.TRANSFER_IN, TransactionStatus.COMPLETED,
                amount, toBalance, TransactionCategory.TRANSFER,
                "External transfer from " + fromAccount.getAccountNumber(), memo, now
        ));

        addLog(userId, ActivityType.EXTERNAL_TRANSFER, "User sent an external transfer of $" + amount + ".");

        User sender = userRepository.findById(userId).orElse(null);
        String senderDescription = sender != null
                ? sender.getFirstName() + " " + Masking.lastInitial(sender.getLastName())
                : "another user";

        return new ExternalTransferResult(outLeg, inLeg, toAccount.getUserId(), senderDescription);
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /**
     * Applies a debit on fromId and a credit on toId, executing whichever row has the smaller
     * account_id first (deterministic lock order — avoids deadlocking against a concurrent
     * transfer running in the opposite direction between the same two accounts). If the second
     * leg fails after the first succeeded, the first leg is compensated (reversed) before the
     * exception propagates, so the transfer is all-or-nothing even though the two UPDATEs are
     * separate statements.
     */
    private void applyTransferLegsInLockOrder(int fromId, int toId, BigDecimal amount, LocalDateTime now, int userId,
            TransactionCategory category, String failDescription) {
        if (fromId < toId) {
            debitOrFail(fromId, amount, now, userId, TransactionType.TRANSFER_OUT, category, failDescription, null);
            try {
                creditOrFail(toId, amount, now);
            } catch (RuntimeException e) {
                accountRepository.creditIfActive(fromId, amount, now); // reverse the debit — fromId is ACTIVE, just debited
                throw e;
            }
        } else {
            creditOrFail(toId, amount, now);
            try {
                debitOrFail(fromId, amount, now, userId, TransactionType.TRANSFER_OUT, category, failDescription, null);
            } catch (RuntimeException e) {
                accountRepository.debitIfSufficient(toId, amount, now); // reverse the credit — toId is ACTIVE, just credited
                throw e;
            }
        }
    }

    private void debitOrFail(int accountId, BigDecimal amount, LocalDateTime now, int userId,
            TransactionType type, TransactionCategory category, String description, String memo) {
        int rows = accountRepository.debitIfSufficient(accountId, amount, now);
        if (rows > 0) {
            return;
        }

        UserBankAccount fresh = accountRepository.findById(accountId);
        if (fresh == null) {
            throw new NotFoundException("Account not found.");
        }
        if (fresh.getStatus() != BankAccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active.");
        }

        // Status is ACTIVE and the conditional UPDATE still affected 0 rows — insufficient funds.
        Transaction failed = new Transaction(
                0, newReference(), accountId, null, type, TransactionStatus.FAILED,
                amount, null, category, description + " (declined — insufficient funds)", memo, now
        );
        transactionRepository.save(failed);
        addLog(userId, activityFor(type), "Declined: insufficient funds for " + type + " of $" + amount + ".");

        throw new InsufficientFundsException("Insufficient funds: available balance is $" + fresh.getBalance() + ".");
    }

    private void creditOrFail(int accountId, BigDecimal amount, LocalDateTime now) {
        int rows = accountRepository.creditIfActive(accountId, amount, now);
        if (rows > 0) {
            return;
        }
        UserBankAccount fresh = accountRepository.findById(accountId);
        if (fresh == null) {
            throw new NotFoundException("Account not found.");
        }
        throw new AccountNotActiveException("Account is not active.");
    }

    private ActivityType activityFor(TransactionType type) {
        return switch (type) {
            case WITHDRAWAL -> ActivityType.WITHDRAWAL;
            case TRANSFER_OUT -> ActivityType.TRANSFER;
            default -> ActivityType.WITHDRAWAL;
        };
    }

    private BigDecimal requireValidAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Amount is required.");
        }
        BigDecimal normalized = MoneyUtil.normalize(amount);
        if (normalized.compareTo(MIN_AMOUNT) < 0) {
            throw new BadRequestException("Amount must be at least $0.01.");
        }
        if (normalized.compareTo(MAX_AMOUNT) > 0) {
            throw new BadRequestException("Amount cannot exceed $1,000,000.00 per operation.");
        }
        return normalized;
    }

    private String newReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private void addLog(int userId, ActivityType activityType, String message) {
        logRepository.save(new Log(0, userId, activityType, message));
    }

    public record TransferResult(Transaction outLeg, Transaction inLeg) {
    }

    public record ExternalTransferResult(Transaction outLeg, Transaction inLeg, int recipientUserId, String senderDescription) {
    }
}
