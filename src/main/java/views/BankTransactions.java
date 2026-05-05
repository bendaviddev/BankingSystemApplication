package main.java.views;

import main.java.enums.ActivityType;
import main.java.enums.TransactionType;
import java.util.ArrayList;
import java.util.Scanner;
import main.java.models.Transaction;
import main.java.models.User;
import main.java.services.BankDataStore;

public class BankTransactions {

    public static void viewTransactions(User user, Scanner scan) {

        boolean viewing = true;

        while (viewing) {
            System.out.println("==== Transaction History ====");
            System.out.println("1. View all transactions");
            System.out.println("2. View deposits only");
            System.out.println("3. View withdrawals only");
            System.out.println("4. View transfers only");
            System.out.println("5. View transactions for one account");
            System.out.println("6. Back");
            System.out.print("Choose an option: ");

            int option = getInt(scan);

            switch (option) {
                case 1:
                    printTransactions(BankDataStore.getTransactionsForUser(user.getId()));
                    BankDataStore.addLog(user.getId(), ActivityType.VIEW_TRANSACTION_HISTORY, "User viewed all transactions.");
                    break;

                case 2:
                    printTransactions(BankDataStore.getTransactionsForUserByType(user.getId(), TransactionType.DEPOSIT));
                    BankDataStore.addLog(user.getId(), ActivityType.VIEW_TRANSACTION_HISTORY, "User viewed deposit transactions.");
                    break;

                case 3:
                    printTransactions(BankDataStore.getTransactionsForUserByType(user.getId(), TransactionType.WITHDRAWAL));
                    BankDataStore.addLog(user.getId(), ActivityType.VIEW_TRANSACTION_HISTORY, "User viewed withdrawal transactions.");
                    break;

                case 4:
                    printTransactions(BankDataStore.getTransactionsForUserByType(user.getId(), TransactionType.TRANSFER));
                    BankDataStore.addLog(user.getId(), ActivityType.VIEW_TRANSACTION_HISTORY, "User viewed transfer transactions.");
                    break;

                case 5:
                    System.out.print("Enter Account ID: ");
                    int accountId = getInt(scan);

                    printTransactions(BankDataStore.getTransactionsForUserByAccount(user.getId(), accountId));
                    BankDataStore.addLog(user.getId(), ActivityType.VIEW_TRANSACTION_HISTORY, "User viewed transactions for account ID " + accountId + ".");
                    break;

                case 6:
                    viewing = false;
                    break;

                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static void printTransactions(ArrayList<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        for (Transaction transaction : transactions) {
            transaction.printTransaction();
            System.out.println("--------------------------");
        }
    }

    private static int getInt(Scanner scan) {
        try {
            return Integer.parseInt(scan.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
