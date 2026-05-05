package main.java.views;

import main.java.enums.ActivityType;
import main.java.models.User;
import main.java.models.UserBankAccount;
import main.java.services.BankDataStore;

import java.util.ArrayList;

public class BankViewAccounts {

    public static void viewAccounts(User user) {
        ArrayList<UserBankAccount> accounts = BankDataStore.getAccountsByUserId(user.getId());
        BankDataStore.addLog(user.getId(), ActivityType.VIEW_BANK_ACCOUNT, "User viewed bank accounts.");

        System.out.println("==== Bank Accounts ====");

        if (accounts.isEmpty()) {
            System.out.println("You do not have any bank accounts yet.");
            return;
        }

        for (UserBankAccount account : accounts) {
            account.printAccount();
            System.out.println("--------------------------");
        }
    }
}
