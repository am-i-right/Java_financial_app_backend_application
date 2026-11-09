package com.myfinance.service;

import static org.junit.jupiter.api.Assertions.*;

import com.myfinance.model.Category;
import com.myfinance.model.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class FinanceManagerTest {
    private final AuthService auth = new AuthService();
    private final FinanceManager finance = new FinanceManager(auth);
    private final LocalDate date = LocalDate.of(2026, 11, 9);

    @Test
    void everyFinancialOperationRequiresAuthentication() {
        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> finance.createCategory("Еда", Category.Type.EXPENSE)),
                () -> assertThrows(IllegalStateException.class, finance::getCategories),
                () -> assertThrows(IllegalStateException.class, () -> finance.addIncome("Зарплата", BigDecimal.TEN, date)),
                () -> assertThrows(IllegalStateException.class, () -> finance.addExpense("Еда", BigDecimal.ONE, date)),
                () -> assertThrows(IllegalStateException.class, finance::getBalance),
                () -> assertThrows(IllegalStateException.class, finance::getTransactions),
                () -> assertThrows(IllegalStateException.class, finance::getTransactionsByAmount),
                () -> assertThrows(IllegalStateException.class, () -> finance.setBudget("Еда", YearMonth.from(date), BigDecimal.TEN)),
                () -> assertThrows(IllegalStateException.class, () -> finance.getBudgetStatuses(YearMonth.from(date))),
                () -> assertThrows(IllegalStateException.class, () -> finance.getAlerts(YearMonth.from(date))),
                () -> assertThrows(IllegalStateException.class, () -> finance.generateReport(new ReportGenerator(), YearMonth.from(date))));
    }

    @Test
    void twoUsersHaveIndependentCategoriesTransactionsAndBudgets() {
        auth.register("alice", "password1");
        auth.register("bob", "password2");
        auth.login("alice", "password1");
        finance.createCategory("Еда", Category.Type.EXPENSE);
        finance.setBudget("Еда", YearMonth.from(date), BigDecimal.TEN);
        finance.addExpense("Еда", BigDecimal.ONE, date);
        auth.logout();
        auth.login("bob", "password2");
        assertEquals(Money.ZERO, finance.getBalance());
        assertTrue(finance.getCategories().isEmpty());
        assertTrue(finance.getTransactions().isEmpty());
        assertTrue(finance.getBudgetStatuses(YearMonth.from(date)).isEmpty());
        finance.createCategory("Еда", Category.Type.EXPENSE);
        finance.addExpense("Еда", BigDecimal.TEN, date);
        auth.logout();
        auth.login("alice", "password1");
        assertEquals(new BigDecimal("-1.00"), finance.getBalance());
        assertEquals(new BigDecimal("1.00"), finance.getBudgetStatuses(YearMonth.from(date)).get(0).spent());
        assertEquals(1, finance.getTransactions().size());
    }

    @Test
    void sortingDoesNotChangeStoredHistory() {
        login();
        finance.createCategory("Зарплата", Category.Type.INCOME);
        finance.addIncome("Зарплата", new BigDecimal("10"), date.plusDays(1));
        finance.addIncome("Зарплата", new BigDecimal("30"), date.minusDays(1));
        finance.addIncome("Зарплата", new BigDecimal("20"), date);
        assertEquals(date.minusDays(1), finance.getTransactions().get(0).getDate());
        assertEquals(new BigDecimal("10.00"), finance.getTransactionsByAmount().get(0).getAmount());
        assertEquals(new BigDecimal("30.00"), finance.getTransactions().get(0).getAmount());
        assertEquals(new BigDecimal("10.00"), auth.getCurrentUser().getWallet().getTransactions().get(0).getAmount());
    }

    @Test
    void invalidOperationLeavesWalletUnchanged() {
        login();
        finance.createCategory("Зарплата", Category.Type.INCOME);
        assertThrows(IllegalArgumentException.class, () -> finance.addExpense("Зарплата", BigDecimal.TEN, date));
        assertThrows(IllegalArgumentException.class, () -> finance.addIncome("Нет", BigDecimal.TEN, date));
        assertTrue(finance.getTransactions().isEmpty());
    }

    @Test
    void reportImplementationCanBeReplacedThroughInterface() {
        login();
        Report report = (wallet, month) -> "Месяц: " + month + ", баланс: " + wallet.getBalance();
        assertEquals("Месяц: 2026-11, баланс: 0.00", finance.generateReport(report, YearMonth.from(date)));
    }

    @Test
    void logoutImmediatelyRevokesFinancialAccess() {
        login();
        auth.logout();
        assertThrows(IllegalStateException.class, finance::getBalance);
    }

    private void login() {
        auth.register("alice", "password1");
        auth.login("alice", "password1");
    }
}
