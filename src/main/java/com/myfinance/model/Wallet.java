package com.myfinance.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Wallet {
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<YearMonth, Map<Category, BigDecimal>> budgets = new LinkedHashMap<>();

    public Category createCategory(String name, Category.Type type) {
        Category category = new Category(name, type);
        String key = categoryKey(name);
        if (categories.containsKey(key)) {
            throw new IllegalArgumentException("Категория с таким названием уже существует.");
        }
        categories.put(key, category);
        return category;
    }

    public Category getCategory(String name) {
        Category category = categories.get(categoryKey(name));
        if (category == null) {
            throw new IllegalArgumentException("Категория не найдена: " + name);
        }
        return category;
    }

    public List<Category> getCategories() {
        return List.copyOf(categories.values());
    }

    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Операция не должна быть null.");
        }
        Category category = transaction.getCategory();
        // Категория должна быть именно объектом этого кошелька, а не другого пользователя.
        if (categories.get(categoryKey(category.getName())) != category) {
            throw new IllegalArgumentException("Категория не принадлежит этому кошельку.");
        }
        transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return List.copyOf(transactions);
    }

    public BigDecimal getBalance() {
        BigDecimal balance = Money.ZERO;
        for (Transaction transaction : transactions) {
            balance = balance.add(transaction.getSignedAmount());
        }
        return balance;
    }

    public void setBudget(String categoryName, YearMonth month, BigDecimal limit) {
        requireMonth(month);
        Category category = getCategory(categoryName);
        if (category.getType() != Category.Type.EXPENSE) {
            throw new IllegalArgumentException("Бюджет можно задать только для категории расходов.");
        }
        BigDecimal checkedLimit = Money.positive(limit);
        budgets.computeIfAbsent(month, ignored -> new LinkedHashMap<>()).put(category, checkedLimit);
    }

    public List<BudgetStatus> getBudgetStatuses(YearMonth month) {
        requireMonth(month);
        List<BudgetStatus> result = new ArrayList<>();
        for (Map.Entry<Category, BigDecimal> entry : budgets.getOrDefault(month, Map.of()).entrySet()) {
            BigDecimal spent = Money.ZERO;
            for (Transaction transaction : transactions) {
                if (transaction instanceof Expense
                        && transaction.getCategory() == entry.getKey()
                        && YearMonth.from(transaction.getDate()).equals(month)) {
                    spent = spent.add(transaction.getAmount());
                }
            }
            result.add(new BudgetStatus(entry.getKey(), month, entry.getValue(), spent));
        }
        return List.copyOf(result);
    }

    public List<String> getAlerts(YearMonth month) {
        requireMonth(month);
        return new Alert().check(month);
    }

    private static String categoryKey(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Укажите название категории.");
        }
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private static void requireMonth(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Укажите месяц.");
        }
    }

    /** Неизменяемый результат расчёта; record — статический вложенный класс. */
    public record BudgetStatus(Category category, YearMonth month, BigDecimal limit, BigDecimal spent) {
        public BigDecimal remaining() {
            return limit.subtract(spent);
        }

        public boolean exceeded() {
            return spent.compareTo(limit) > 0;
        }
    }

    /** Inner class имеет доступ к данным конкретного Wallet, поэтому static здесь не нужен. */
    private final class Alert {
        private List<String> check(YearMonth month) {
            List<String> messages = new ArrayList<>();
            for (BudgetStatus status : getBudgetStatuses(month)) {
                if (status.exceeded()) {
                    messages.add("Превышен бюджет категории «" + status.category().getName()
                            + "» за " + month + " на " + status.remaining().negate().toPlainString() + " RUB.");
                }
            }
            if (getBalance().signum() < 0) {
                messages.add("Общие расходы превысили доходы: баланс кошелька отрицательный.");
            }
            return List.copyOf(messages);
        }
    }
}
