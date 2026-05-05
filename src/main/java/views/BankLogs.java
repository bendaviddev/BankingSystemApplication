package main.java.views;

import java.util.ArrayList;

import main.java.models.Log;
import main.java.models.User;
import main.java.services.BankDataStore;

public class BankLogs {

    public static void viewLogs(User user) {
        ArrayList<Log> logs = BankDataStore.getLogsByUserId(user.getId());

        System.out.println("==== Activity Logs ====");

        if (logs.isEmpty()) {
            System.out.println("No logs found.");
            return;
        }

        for (Log log : logs) {
            log.printLog();
            System.out.println("--------------------------");
        }
    }
}
