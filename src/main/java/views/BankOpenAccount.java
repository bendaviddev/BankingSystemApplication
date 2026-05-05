package main.java.views;

import main.java.enums.BankAccountType;
import java.util.Scanner;
import main.java.models.User;
import main.java.models.UserBankAccount;
import main.java.services.BankDataStore;
import main.java.util.BankUtil;

public class BankOpenAccount {

    public static void openAccount(User user, Scanner scan) {
        System.out.println("==== Open New Bank Account ====");
        System.out.println("1. Basic Account");
        System.out.println("2. Saving Account");
        System.out.println("Choose account type: ");
        int choice = getInt(scan);

        BankAccountType type;
        if (choice == 1) {
            type = BankAccountType.BASIC;
        } else if (choice == 2) {
            type = BankAccountType.SAVING;
        } else {
            System.out.println("Invalid account type.");
            return;
        }
        System.out.println("Opening Balance: $");
        double openingBalance = getDouble(scan);

        if (!BankUtil.isValidOpeningBalance(type, openingBalance)) {
            System.out.printf("Invalid opening balance. %s accounts require at least $%.2f.%n", type, BankUtil.getMinimumBalance(type));
            return;
        }

        UserBankAccount account = BankDataStore.openAccount(user.getId(), type, openingBalance);
        System.out.println("Account opened successfully!");
        account.printAccount();
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
