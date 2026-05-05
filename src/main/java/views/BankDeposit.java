package main.java.views;

import java.util.Scanner;
import main.java.models.Transaction;
import main.java.models.User;
import main.java.models.UserBankAccount;
import main.java.services.BankDataStore;

public class BankDeposit {

    public static void deposit(User user, Scanner scan) {

        BankViewAccounts.viewAccounts(user);

        System.out.println("==== Deposit ====");
        System.out.print("Account ID: ");
        int accountId = getInt(scan);

        System.out.print("Amount: $");
        double amount = getDouble(scan);

        boolean success = BankDataStore.deposit(user.getId(), accountId, amount);

        if (success) {
            System.out.println("Deposit successful.");

            UserBankAccount account = BankDataStore.getAccountByIdForUser(accountId, user.getId());
            Transaction transaction = BankDataStore.getLatestTransactionForAccount(accountId);

            BankDataStore.printTransactionReceipt(transaction, account);
        } else {
            System.out.println("Deposit failed. Check the account ID and amount.");
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
