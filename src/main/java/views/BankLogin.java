package main.java.views;

import java.util.Scanner;

import main.java.models.User;
import main.java.services.BankDataStore;
import main.java.util.PasswordUtil;

public class BankLogin {

    public static void login(Scanner scan) {
        System.out.println("==== Login ====");

        System.out.println("Enter username: ");
        String username = scan.nextLine();

        String password = PasswordUtil.readPassword("Enter password: ", scan);

        User user = BankDataStore.loginUser(username, password);

        System.out.println("Checking database for user...");

        if (user != null) {
            System.out.println("Login successful!");

            if (user.isAdmin()) {
                BankAdminDashboard.showAdminDashboard(user, scan);
            } else {
                BankUserProfile.showProfile(user, scan);
            }

        } else {
            System.out.println("Invalid username or password.");
        }
    }
}
