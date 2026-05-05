package main.java.views;

import main.java.enums.ActivityType;
import java.util.Scanner;
import main.java.models.User;
import main.java.services.BankDataStore;

public class BankChangeUserProfileInfo {

    public static void changeInfo(User user, Scanner scan) {

        System.out.println("==== Change Profile Information ====");
        System.out.print("First name: ");
        String firstName = scan.nextLine();

        System.out.print("Last name: ");
        String lastName = scan.nextLine();

        System.out.print("Phone number: ");
        String phoneNumber = scan.nextLine();

        System.out.print("Email: ");
        String email = scan.nextLine();

        System.out.print("Address: ");
        String address = scan.nextLine();

        BankDataStore.updateUserProfile(user, firstName, lastName, phoneNumber, email, address);
        BankDataStore.addLog(user.getId(), ActivityType.UPDATE_PERSONAL_INFORMATION, "User updated profile information.");
        System.out.println("Profile updated successfully.");
    }
}
