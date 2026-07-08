package com.benbanking.api.seed;

import com.benbanking.api.enums.AlertSeverity;
import com.benbanking.api.enums.AlertType;
import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.Currency;
import com.benbanking.api.enums.TransactionCategory;
import com.benbanking.api.enums.TransactionStatus;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.enums.UserRole;
import com.benbanking.api.models.Alert;
import com.benbanking.api.models.Transaction;
import com.benbanking.api.models.User;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.repositories.AccountRepository;
import com.benbanking.api.repositories.AlertRepository;
import com.benbanking.api.repositories.TransactionRepository;
import com.benbanking.api.repositories.UserRepository;
import com.benbanking.api.util.PasswordUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Seeds demo data on first boot (any profile — H2 or MySQL), only when the users table is
 * empty. Uses a fixed Random seed so the generated history is reproducible across runs.
 *
 * Money movement here is generated directly against the repositories (not through
 * AccountService) so that transaction created_at timestamps can be spread across the past
 * ~90 days instead of all landing on "now" — the running balance is tracked in Java as we
 * go and each account's final balance is written once at the end.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final long RANDOM_SEED = 42L;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;

    public DataSeeder(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AlertRepository alertRepository
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.countAll() > 0) {
            return;
        }

        Random random = new Random(RANDOM_SEED);

        User demo = createUser("demo", "Demo123!", "Demo", "User",
                "555-0100", "demo@example.com", "1 Market St, San Francisco, CA", UserRole.USER);
        User sofia = createUser("sofia", "Sofia123!", "Sofia", "Alvarez",
                "555-0142", "sofia@example.com", "22 Baker St, Chicago, IL", UserRole.USER);
        createUser("admin", "Admin123!", "Ada", "Admin",
                "555-0111", "admin@example.com", "1 Admin Way, Austin, TX", UserRole.ADMIN);

        UserBankAccount demoChecking = openSeedAccount(demo.getId(), BankAccountType.CHECKING, 90);
        UserBankAccount demoSavings = openSeedAccount(demo.getId(), BankAccountType.SAVINGS, 90);
        seedDemoHistory(demo.getId(), demoChecking, demoSavings, random);

        UserBankAccount sofiaChecking = openSeedAccount(sofia.getId(), BankAccountType.CHECKING, 45);
        seedSofiaHistory(sofia.getId(), sofiaChecking, random);

        seedAlerts(demo.getId(), demoChecking);
    }

    private User createUser(String username, String password, String firstName, String lastName,
            String phone, String email, String address, UserRole role) {
        User user = new User(0, username, PasswordUtil.hashPassword(password), firstName, lastName,
                phone, email, address, role, null, null);
        return userRepository.save(user);
    }

    private UserBankAccount openSeedAccount(int userId, BankAccountType type, int daysAgo) {
        String accountNumber = "ACCT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(daysAgo);
        UserBankAccount account = new UserBankAccount(
                0, userId, accountNumber, type, Currency.DOLLAR, BigDecimal.ZERO, BankAccountStatus.ACTIVE,
                createdAt, createdAt
        );
        return accountRepository.save(account);
    }

    private void seedDemoHistory(int userId, UserBankAccount checking, UserBankAccount savings, Random random) {
        LocalDateTime seedStart = LocalDateTime.now().minusDays(90);
        BigDecimal checkingBalance = BigDecimal.ZERO;
        BigDecimal savingsBalance = BigDecimal.ZERO;

        for (int day = 0; day < 90; day++) {
            LocalDateTime dayStart = seedStart.plusDays(day);

            if (day == 0 || day % 30 == 0) {
                BigDecimal salary = amount(random, 3000, 3400);
                checkingBalance = deposit(checking.getAccountId(), checkingBalance, salary,
                        TransactionCategory.INCOME, "Salary deposit", dayStart.withHour(9));
            }

            if (day % 30 == 3) {
                BigDecimal rent = amount(random, 1150, 1350);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, rent,
                        TransactionCategory.RENT, "Rent payment", dayStart.withHour(10));
            }

            if (day % 30 == 5) {
                BigDecimal utilities = amount(random, 80, 160);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, utilities,
                        TransactionCategory.UTILITIES, "Utility bill", dayStart.withHour(11));
            }

            if (day % 7 == 2 || day % 7 == 5) {
                BigDecimal groceries = amount(random, 35, 130);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, groceries,
                        TransactionCategory.GROCERIES, "Grocery shopping", dayStart.withHour(18));
            }

            if (random.nextDouble() < 0.35) {
                BigDecimal dining = amount(random, 12, 65);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, dining,
                        TransactionCategory.DINING, "Dining out", dayStart.withHour(19));
            }

            if (random.nextDouble() < 0.15) {
                BigDecimal entertainment = amount(random, 10, 85);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, entertainment,
                        TransactionCategory.ENTERTAINMENT, "Entertainment", dayStart.withHour(20));
            }

            if (random.nextDouble() < 0.10) {
                BigDecimal shopping = amount(random, 20, 220);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, shopping,
                        TransactionCategory.SHOPPING, "Online shopping", dayStart.withHour(14));
            }

            if (day % 7 == 1) {
                BigDecimal transport = amount(random, 20, 65);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, transport,
                        TransactionCategory.TRANSPORT, "Transit pass", dayStart.withHour(8));
            }

            if (day % 45 == 12) {
                BigDecimal health = amount(random, 25, 180);
                checkingBalance = withdraw(userId, checking.getAccountId(), checkingBalance, health,
                        TransactionCategory.HEALTH, "Pharmacy / clinic visit", dayStart.withHour(15));
            }

            if (day % 30 == 15 && checkingBalance.compareTo(new BigDecimal("400")) > 0) {
                BigDecimal moveToSavings = new BigDecimal("300.00");
                String outRef = newReference();
                String inRef = newReference();
                LocalDateTime moment = dayStart.withHour(12);

                checkingBalance = checkingBalance.subtract(moveToSavings);
                transactionRepository.save(new Transaction(0, outRef, checking.getAccountId(), savings.getAccountId(),
                        TransactionType.TRANSFER_OUT, TransactionStatus.COMPLETED, moveToSavings, checkingBalance,
                        TransactionCategory.TRANSFER, "Transfer to savings", null, moment));

                savingsBalance = savingsBalance.add(moveToSavings);
                transactionRepository.save(new Transaction(0, inRef, savings.getAccountId(), checking.getAccountId(),
                        TransactionType.TRANSFER_IN, TransactionStatus.COMPLETED, moveToSavings, savingsBalance,
                        TransactionCategory.TRANSFER, "Transfer from checking", null, moment));
            }

            if (day % 30 == 20 && day > 0) {
                BigDecimal interest = amount(random, 15, 60);
                savingsBalance = savingsBalance.add(interest);
                transactionRepository.save(new Transaction(0, newReference(), savings.getAccountId(), null,
                        TransactionType.DEPOSIT, TransactionStatus.COMPLETED, interest, savingsBalance,
                        TransactionCategory.INCOME, "Savings interest", null, dayStart.withHour(9)));
            }
        }

        accountRepository.setBalance(checking.getAccountId(), checkingBalance.setScale(2, RoundingMode.HALF_EVEN), LocalDateTime.now());
        accountRepository.setBalance(savings.getAccountId(), savingsBalance.setScale(2, RoundingMode.HALF_EVEN), LocalDateTime.now());
    }

    private void seedSofiaHistory(int userId, UserBankAccount checking, Random random) {
        LocalDateTime seedStart = LocalDateTime.now().minusDays(45);
        BigDecimal balance = BigDecimal.ZERO;

        balance = deposit(checking.getAccountId(), balance, new BigDecimal("2200.00"),
                TransactionCategory.INCOME, "Salary deposit", seedStart.withHour(9));

        for (int day = 5; day < 45; day += 6) {
            LocalDateTime moment = seedStart.plusDays(day).withHour(17);
            BigDecimal spend = amount(random, 20, 90);
            balance = withdraw(userId, checking.getAccountId(), balance, spend,
                    TransactionCategory.GROCERIES, "Grocery shopping", moment);
        }

        balance = deposit(checking.getAccountId(), balance, new BigDecimal("2200.00"),
                TransactionCategory.INCOME, "Salary deposit", seedStart.plusDays(30).withHour(9));

        accountRepository.setBalance(checking.getAccountId(), balance.setScale(2, RoundingMode.HALF_EVEN), LocalDateTime.now());
    }

    private void seedAlerts(int userId, UserBankAccount checking) {
        alertRepository.save(new Alert(0, userId, AlertType.LARGE_TRANSACTION, AlertSeverity.INFO,
                "Large transaction of $3,200.00 recorded.", false, LocalDateTime.now().minusDays(2)));
        alertRepository.save(new Alert(0, userId, AlertType.SECURITY, AlertSeverity.INFO,
                "Welcome to Ben Banking — this is a demo account with simulated funds only.", true,
                LocalDateTime.now().minusDays(5)));
    }

    private BigDecimal deposit(int accountId, BigDecimal currentBalance, BigDecimal amount,
            TransactionCategory category, String description, LocalDateTime when) {
        BigDecimal newBalance = currentBalance.add(amount).setScale(2, RoundingMode.HALF_EVEN);
        transactionRepository.save(new Transaction(0, newReference(), accountId, null,
                TransactionType.DEPOSIT, TransactionStatus.COMPLETED, amount, newBalance,
                category, description, null, when));
        return newBalance;
    }

    private BigDecimal withdraw(int userId, int accountId, BigDecimal currentBalance, BigDecimal amount,
            TransactionCategory category, String description, LocalDateTime when) {
        // Keep a small buffer so seeded history never dips into a synthetic overdraft.
        BigDecimal safeAmount = amount.min(currentBalance.subtract(new BigDecimal("50")));
        if (safeAmount.compareTo(new BigDecimal("1.00")) < 0) {
            return currentBalance;
        }
        BigDecimal newBalance = currentBalance.subtract(safeAmount).setScale(2, RoundingMode.HALF_EVEN);
        transactionRepository.save(new Transaction(0, newReference(), accountId, null,
                TransactionType.WITHDRAWAL, TransactionStatus.COMPLETED, safeAmount, newBalance,
                category, description, null, when));
        return newBalance;
    }

    private BigDecimal amount(Random random, int minWhole, int maxWhole) {
        int whole = minWhole + random.nextInt(maxWhole - minWhole + 1);
        int cents = random.nextInt(100);
        return new BigDecimal(whole).add(new BigDecimal(cents).movePointLeft(2)).setScale(2, RoundingMode.HALF_EVEN);
    }

    private String newReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
