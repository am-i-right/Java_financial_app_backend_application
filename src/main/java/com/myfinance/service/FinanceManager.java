package com.myfinance.service;

import com.myfinance.model.Category;
import com.myfinance.model.Expense;
import com.myfinance.model.Income;
import com.myfinance.model.Transaction;
import com.myfinance.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Доступ к финансам всегда через текущего авторизованного пользователя. */
public final class FinanceManager {
    private final AuthService auth;

    public FinanceManager(AuthService auth) {
        this.auth = Objects.requireNonNull(auth);
    }

    public void createCategory(String name, Category.Type type) {
        wallet().createCategory(name, type);
    }

    public List<Category> getCategories() {
        return wallet().getCategories();
    }

    public void addIncome(String categoryName, BigDecimal amount, LocalDate date) {
        Wallet wallet = wallet();
        wallet.addTransaction(new Income(amount, date, wallet.getCategory(categoryName)));
    }

    public void addExpense(String categoryName, BigDecimal amount, LocalDate date) {
        Wallet wallet = wallet();
        wallet.addTransaction(new Expense(amount, date, wallet.getCategory(categoryName)));
    }

    public BigDecimal getBalance() {
        return wallet().getBalance();
    }

    public void setBudget(String categoryName, YearMonth month, BigDecimal limit) {
        wallet().setBudget(categoryName, month, limit);
    }

    public List<Wallet.BudgetStatus> getBudgetStatuses(YearMonth month) {
        return wallet().getBudgetStatuses(month);
    }

    public List<String> getAlerts(YearMonth month) {
        return wallet().getAlerts(month);
    }

    public List<Transaction> getTransactions() {
        return sortedTransactions(new Transaction.DateComparator());
    }

    public List<Transaction> getTransactionsByAmount() {
        // Учебный пример anonymous class. В обычном проекте достаточно Comparator.comparing(...).
        return sortedTransactions(new Comparator<Transaction>() {
            @Override
            public int compare(Transaction first, Transaction second) {
                return first.getAmount().compareTo(second.getAmount());
            }
        });
    }

    public String generateReport(Report report, YearMonth month) {
        return Objects.requireNonNull(report).generate(wallet(), month);
    }

    private List<Transaction> sortedTransactions(Comparator<Transaction> comparator) {
        List<Transaction> result = new ArrayList<>(wallet().getTransactions());
        result.sort(comparator);
        return List.copyOf(result);
    }

    private Wallet wallet() {
        return auth.getCurrentUser().getWallet();
    }
}
