package main.java.views;

import main.java.enums.ActivityType;
import java.util.Scanner;
import main.java.models.User;
import main.java.services.BankDataStore;

public class BankUserProfile {

    public static void showProfile(User user, Scanner scan) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("=================================");
            System.out.println("User Profile: " + user.getUsername());
            System.out.println("Name: " + user.getFullName());
            System.out.println("=================================");

            System.out.println("1. Change Profile Information");
            System.out.println("2. View Bank Accounts");
            System.out.println("3. Open New Bank Account");
            System.out.println("4. Deposit");
            System.out.println("5. Withdraw");
            System.out.println("6. Transfer");
            System.out.println("7. View Transactions");
            System.out.println("8. View Logs");
            System.out.println("9. Logout");

            System.out.println("Choose an option: ");
            int option = getInt(scan);
            switch (option) {
                case 1:
                    BankChangeUserProfileInfo.changeInfo(user, scan);
                    break;
                case 2:
                    BankViewAccounts.viewAccounts(user);
                    break;
                case 3:
                    BankOpenAccount.openAccount(user, scan);
                    break;
                case 4:
                    BankDeposit.deposit(user, scan);
                    break;
                case 5:
                    BankWithdraw.withdraw(user, scan);
                    break;
                case 6:
                    BankTransfer.transfer(user, scan);
                    break;
                case 7:
                    BankTransactions.viewTransactions(user, scan);
                    break;
                case 8:
                    BankLogs.viewLogs(user);
                    break;
                case 9:
                    BankDataStore.addLog(user.getId(), ActivityType.LOGOUT, "User logged out.");
                    System.out.println("Logged out successfully.");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
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
