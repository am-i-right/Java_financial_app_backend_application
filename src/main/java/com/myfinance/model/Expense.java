package com.myfinance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Expense extends Transaction {
    public Expense(BigDecimal amount, LocalDate date, Category category) {
        super(amount, date, category, Category.Type.EXPENSE);
    }

    @Override
    public BigDecimal getSignedAmount() {
        return getAmount().negate();
    }
}
