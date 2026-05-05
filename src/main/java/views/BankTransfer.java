package main.java.views;

import java.util.Scanner;
import main.java.models.Transaction;
import main.java.models.User;
import main.java.models.UserBankAccount;
import main.java.services.BankDataStore;

public class BankTransfer {

    public static void transfer(User user, Scanner scan) {

        BankViewAccounts.viewAccounts(user);

        System.out.println("==== Transfer ====");
        System.out.print("From Account ID: ");
        int fromAccountId = getInt(scan);

        System.out.print("To Account ID: ");
        int toAccountId = getInt(scan);

        System.out.print("Amount: $");
        double amount = getDouble(scan);

        boolean success = BankDataStore.transfer(user.getId(), fromAccountId, toAccountId, amount);

        if (success) {
            System.out.println("Transfer successful.");

            UserBankAccount fromAccount = BankDataStore.getAccountByIdForUser(fromAccountId, user.getId());
            Transaction transaction = BankDataStore.getLatestTransactionForAccount(fromAccountId);

            BankDataStore.printTransactionReceipt(transaction, fromAccount);
        } else {
            System.out.println("Transfer failed. Check the account IDs and amount.");
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
