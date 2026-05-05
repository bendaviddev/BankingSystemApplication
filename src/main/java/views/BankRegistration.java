package main.java.views;

import java.util.Scanner;
import main.java.models.User;
import main.java.services.BankDataStore;
import main.java.util.PasswordUtil;

public class BankRegistration {

    public static void register(Scanner scan) {

        System.out.println("==== Registration ====");

        System.out.println("Username: ");
        String username = scan.nextLine();

        PasswordUtil.printPasswordRequirements();

        String password = PasswordUtil.readPassword("Password: ", scan);

        while (!PasswordUtil.isValidPassword(password)) {
            System.out.println("Invalid password.");
            PasswordUtil.printPasswordRequirements();
            password = PasswordUtil.readPassword("Password: ", scan);
        }

        System.out.println("First name: ");
        String firstName = scan.nextLine();

        System.out.println("Last name: ");
        String lastName = scan.nextLine();

        System.out.println("Phone number: ");
        String phoneNumber = scan.nextLine();

        System.out.println("Email: ");
        String email = scan.nextLine();

        System.out.println("Address: ");
        String address = scan.nextLine();

        User user = BankDataStore.registerUser(username, password, firstName, lastName, phoneNumber, email, address);

        if (user == null) {
            System.out.println("That username already exists. Try another username.");
        } else {
            System.out.println("User registered successfully!");
            System.out.println("Welcome, " + user.getFullName() + ".");
        }
    }
}
