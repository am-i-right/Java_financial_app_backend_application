package com.myfinance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Income extends Transaction {
    public Income(BigDecimal amount, LocalDate date, Category category) {
        super(amount, date, category, Category.Type.INCOME);
    }

    @Override
    public BigDecimal getSignedAmount() {
        return getAmount();
    }
}
