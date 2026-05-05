package main.java.views;

import java.util.Scanner;

import main.java.enums.BankAccountStatus;
import main.java.models.User;
import main.java.services.BankDataStore;

public class BankAdminDashboard {

    public static void showAdminDashboard(User user, Scanner scan) {

        if (user == null || !user.isAdmin()) {
            System.out.println("Access denied. Admins only.");
            return;
        }

        boolean run = true;

        while (run) {
            System.out.println("=================================");
            System.out.println("Admin Dashboard");
            System.out.println("=================================");
            System.out.println("1. View All Users");
            System.out.println("2. View All Bank Accounts");
            System.out.println("3. View All Transactions");
            System.out.println("4. View All Logs");
            System.out.println("5. Freeze Account");
            System.out.println("6. Close Account");
            System.out.println("7. Reopen Account");
            System.out.println("8. Logout");
            System.out.print("Choose an option: ");

            int option = getInt(scan);

            switch (option) {
                case 1:
                    BankDataStore.printAllUsers();
                    break;
                case 2:
                    BankDataStore.printAllAccounts();
                    break;
                case 3:
                    BankDataStore.printAllTransactions();
                    break;
                case 4:
                    BankDataStore.printAllLogs();
                    break;
                case 5:
                    changeAccountStatus(scan, BankAccountStatus.FROZEN);
                    break;
                case 6:
                    changeAccountStatus(scan, BankAccountStatus.CLOSED);
                    break;
                case 7:
                    changeAccountStatus(scan, BankAccountStatus.ACTIVE);
                    break;
                case 8:
                    System.out.println("Admin logged out.");
                    run = false;
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void changeAccountStatus(Scanner scan, BankAccountStatus status) {
        BankDataStore.printAllAccounts();

        System.out.print("Enter Account ID: ");
        int accountId = getInt(scan);

        boolean success = BankDataStore.updateAccountStatus(accountId, status);

        if (success) {
            System.out.println("Account status changed to " + status + ".");
        } else {
            System.out.println("Account status change failed. Check the account ID.");
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
