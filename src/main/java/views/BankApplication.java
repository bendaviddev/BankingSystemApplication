package main.java.views;

import java.util.Scanner;

public class BankApplication {

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        boolean run = true;

        while (run) {
            System.out.println("==================================");
            System.out.println("Welcome to the Banking System");
            System.out.println("==================================");
            System.out.println("1. Login\n2. Register\n3. Exit");
            System.out.println("Choose an option: ");
            int input = getInt(scan);

            switch (input) {
                case 1:
                    BankLogin.login(scan);
                    break;
                case 2:
                    BankRegistration.register(scan);
                    break;
                case 3:
                    System.out.println("Thank you for using the Banking System!");
                    run = false;
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }

        scan.close();
    }

    private static int getInt(Scanner scan) {
        try {
            return Integer.parseInt(scan.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
