package main.java.util;

import main.java.enums.BankAccountType;

public class BankUtil {
    public static double getMinimumBalance(BankAccountType type){
    if(type == BankAccountType.BASIC){
        return 0;
    } else{
        return 500;
    }
}

public static double getFeePercentage(BankAccountType type){
    if(type == BankAccountType.BASIC){
        return 0.02;
    } else{
        return 0.03;
    }
}

public static double getInterestRate(BankAccountType type){
    if(type == BankAccountType.BASIC){
        return 0.04;
    } else{
        return 0.05;
    }
}

public static boolean isValidOpeningBalance(BankAccountType type, double balance){
    return balance >= getMinimumBalance(type);
}
}