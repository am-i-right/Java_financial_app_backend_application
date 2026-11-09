package com.myfinance.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionTest {
    private final Category income = new Category("Зарплата", Category.Type.INCOME);
    private final Category expense = new Category("Еда", Category.Type.EXPENSE);
    private final LocalDate date = LocalDate.of(2026, 11, 9);

    @Test
    void incomeAndExpenseHaveOppositeEffectsOnBalance() {
        Transaction first = new Income(new BigDecimal("120.50"), date, income);
        Transaction second = new Expense(new BigDecimal("20.25"), date, expense);
        assertEquals(new BigDecimal("120.50"), first.getSignedAmount());
        assertEquals(new BigDecimal("-20.25"), second.getSignedAmount());
        assertEquals(date, second.getDate());
        assertSame(expense, second.getCategory());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.001", "12.345"})
    void rejectsInvalidAmounts(String amount) {
        assertThrows(IllegalArgumentException.class,
                () -> new Income(new BigDecimal(amount), date, income));
    }

    @Test
    void normalizesScaleWithoutRounding() {
        assertEquals(new BigDecimal("12.00"), new Income(new BigDecimal("12"), date, income).getAmount());
        assertEquals(new BigDecimal("12.10"), Money.positive(new BigDecimal("12.100")));
    }

    @Test
    void rejectsMissingFieldsAndWrongCategoryTypes() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new Income(null, date, income)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Income(BigDecimal.ONE, null, income)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Income(BigDecimal.ONE, date, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Income(BigDecimal.ONE, date, expense)),
                () -> assertThrows(IllegalArgumentException.class, () -> new Expense(BigDecimal.ONE, date, income)));
    }

    @Test
    void comparatorSortsChronologically() {
        Transaction first = new Income(BigDecimal.ONE, date.minusDays(1), income);
        Transaction second = new Income(BigDecimal.TEN, date, income);
        assertTrue(new Transaction.DateComparator().compare(first, second) < 0);
        assertEquals(0, new Transaction.DateComparator().compare(second, second));
    }
}
