package com.benbanking.api.services;

import com.benbanking.api.dto.AccountLookupResponse;
import com.benbanking.api.enums.AlertType;
import com.benbanking.api.enums.BankAccountStatus;
import com.benbanking.api.enums.BankAccountType;
import com.benbanking.api.enums.TransactionType;
import com.benbanking.api.exceptions.BadRequestException;
import com.benbanking.api.exceptions.NotFoundException;
import com.benbanking.api.models.Alert;
import com.benbanking.api.models.User;
import com.benbanking.api.models.UserBankAccount;
import com.benbanking.api.repositories.AlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExternalTransferTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertService alertService;

    private User newUser(String prefix) {
        String username = prefix + "_" + System.nanoTime();
        return authService.registerUser(username, "Demo123!", "First" + prefix, "Lastname", "555-0100",
                username + "@example.com", "1 Test St");
    }

    @Test
    void lookupReturnsMaskedContractForActiveAccount() {
        User owner = newUser("lookup_owner");
        UserBankAccount account = accountService.openAccount(owner.getId(), BankAccountType.CHECKING, new BigDecimal("100.00"));

        AccountLookupResponse response = accountService.lookupAccount(account.getAccountNumber());

        assertTrue(response.getMaskedAccountNumber().startsWith("•••• "));
        assertFalse(response.getMaskedAccountNumber().contains(account.getAccountNumber()));
        assertEquals("Firstlookup_owner", response.getOwnerFirstName());
        assertEquals("L.", response.getOwnerLastInitial());
    }

    @Test
    void lookupMissingAccountReturns404Equivalent() {
        assertThrows(NotFoundException.class, () -> accountService.lookupAccount("ACCT-DOESNOTEXIST"));
    }

    @Test
    void lookupInactiveAccountIsTreatedAsMissing() {
        User owner = newUser("lookup_inactive");
        User admin = newUser("lookup_admin");
        UserBankAccount account = accountService.openAccount(owner.getId(), BankAccountType.CHECKING, new BigDecimal("100.00"));
        accountService.updateAccountStatus(admin.getId(), account.getAccountId(), BankAccountStatus.FROZEN);

        assertThrows(NotFoundException.class, () -> accountService.lookupAccount(account.getAccountNumber()));
    }

    @Test
    void externalTransferToInactiveRecipientIsRejected() {
        User sender = newUser("ext_sender");
        User recipient = newUser("ext_recipient_inactive");
        User admin = newUser("ext_admin");
        UserBankAccount senderAccount = accountService.openAccount(sender.getId(), BankAccountType.CHECKING, new BigDecimal("500.00"));
        UserBankAccount recipientAccount = accountService.openAccount(recipient.getId(), BankAccountType.CHECKING, new BigDecimal("0.00"));
        accountService.updateAccountStatus(admin.getId(), recipientAccount.getAccountId(), BankAccountStatus.FROZEN);

        assertThrows(NotFoundException.class, () -> accountService.transferExternal(
                sender.getId(), senderAccount.getAccountId(), recipientAccount.getAccountNumber(), new BigDecimal("25.00"), null));
    }

    @Test
    void externalTransferToOwnAccountIsRejected() {
        User sender = newUser("ext_self");
        UserBankAccount from = accountService.openAccount(sender.getId(), BankAccountType.CHECKING, new BigDecimal("500.00"));
        UserBankAccount toOwnSavings = accountService.openAccount(sender.getId(), BankAccountType.SAVINGS, new BigDecimal("0.00"));

        assertThrows(BadRequestException.class, () -> accountService.transferExternal(
                sender.getId(), from.getAccountId(), toOwnSavings.getAccountNumber(), new BigDecimal("25.00"), null));
    }

    @Test
    void externalTransferWritesBothLegsAndAlertsRecipient() {
        User sender = newUser("ext_ok_sender");
        User recipient = newUser("ext_ok_recipient");
        UserBankAccount senderAccount = accountService.openAccount(sender.getId(), BankAccountType.CHECKING, new BigDecimal("500.00"));
        UserBankAccount recipientAccount = accountService.openAccount(recipient.getId(), BankAccountType.CHECKING, new BigDecimal("10.00"));

        AccountService.ExternalTransferResult result = accountService.transferExternal(
                sender.getId(), senderAccount.getAccountId(), recipientAccount.getAccountNumber(), new BigDecimal("75.00"), "gift");

        assertEquals(TransactionType.TRANSFER_OUT, result.outLeg().getTransactionType());
        assertEquals(TransactionType.TRANSFER_IN, result.inLeg().getTransactionType());
        assertEquals(new BigDecimal("425.00"), result.outLeg().getRunningBalance());
        assertEquals(new BigDecimal("85.00"), result.inLeg().getRunningBalance());
        assertEquals(recipient.getId(), result.recipientUserId());

        // Mirrors what AccountController does after the @Transactional method returns (invariant #11).
        alertService.fireTransferReceived(result.recipientUserId(), result.outLeg().getAmount(), result.senderDescription());
        List<Alert> alerts = alertRepository.findByUserId(recipient.getId(), 10);
        assertTrue(alerts.stream().anyMatch(a -> a.getAlertType() == AlertType.TRANSFER_RECEIVED));
    }
}
