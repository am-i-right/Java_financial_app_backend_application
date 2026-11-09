package com.myfinance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Общая проверка сумм: не округляем деньги молча и не используем double. */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    public static BigDecimal positive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Сумма должна содержать не более двух знаков после запятой.");
        }
    }
}
