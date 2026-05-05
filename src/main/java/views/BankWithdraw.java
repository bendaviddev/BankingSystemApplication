package main.java.views;

import java.util.Scanner;
import main.java.models.Transaction;
import main.java.models.User;
import main.java.models.UserBankAccount;
import main.java.services.BankDataStore;

public class BankWithdraw {

    public static void withdraw(User user, Scanner scan) {

        BankViewAccounts.viewAccounts(user);

        System.out.println("==== Withdraw ====");
        System.out.print("Account ID: ");
        int accountId = getInt(scan);

        System.out.print("Amount: $");
        double amount = getDouble(scan);

        boolean success = BankDataStore.withdraw(user.getId(), accountId, amount);

        if (success) {
            System.out.println("Withdrawal successful.");

            UserBankAccount account = BankDataStore.getAccountByIdForUser(accountId, user.getId());
            Transaction transaction = BankDataStore.getLatestTransactionForAccount(accountId);

            BankDataStore.printTransactionReceipt(transaction, account);
        } else {
            System.out.println("Withdrawal failed. Check the account ID, amount, and balance.");
        }
    }

    private static int getInt(Scanner scan) {
        try {
            return Integer.parseInt(scan.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double getDouble(Scanner scan) {
        try {
            return Double.parseDouble(scan.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
