package com.myfinance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

public abstract class Transaction {
    private final BigDecimal amount;
    private final LocalDate date;
    private final Category category;

    protected Transaction(BigDecimal amount, LocalDate date, Category category, Category.Type expectedType) {
        this.amount = Money.positive(amount);
        if (date == null || category == null) {
            throw new IllegalArgumentException("Укажите дату и категорию операции.");
        }
        if (category.getType() != expectedType) {
            throw new IllegalArgumentException("Тип категории не соответствует операции.");
        }
        this.date = date;
        this.category = category;
    }

    public final BigDecimal getAmount() {
        return amount;
    }

    public final LocalDate getDate() {
        return date;
    }

    public final Category getCategory() {
        return category;
    }

    /** Полиморфизм: доход увеличивает баланс, расход уменьшает. */
    public abstract BigDecimal getSignedAmount();

    /** Static nested class: сортировка не требует экземпляра Transaction. */
    public static final class DateComparator implements Comparator<Transaction> {
        @Override
        public int compare(Transaction first, Transaction second) {
            return first.getDate().compareTo(second.getDate());
        }
    }
}
