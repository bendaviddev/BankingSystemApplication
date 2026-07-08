package com.benbanking.api.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

    private MoneyUtil() {
    }

    /** Normalize a client-supplied amount to 2 decimal places using banker's rounding. */
    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
