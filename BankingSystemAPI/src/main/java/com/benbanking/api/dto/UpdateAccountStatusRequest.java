package com.benbanking.api.dto;

import com.benbanking.api.enums.BankAccountStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateAccountStatusRequest {

    @NotNull
    private BankAccountStatus status;

    public BankAccountStatus getStatus() {
        return status;
    }
}
