package com.myfinance.model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalletTest {
    private final Wallet wallet = new Wallet();
    private final YearMonth month = YearMonth.of(2026, 12);
    private Category salary;
    private Category food;

    @BeforeEach
    void createCategories() {
        salary = wallet.createCategory("Зарплата", Category.Type.INCOME);
        food = wallet.createCategory("Еда", Category.Type.EXPENSE);
    }

    @Test
    void sumsMoneyExactlyAndAllowsNegativeBalance() {
        wallet.addTransaction(new Income(new BigDecimal("0.10"), month.atDay(1), salary));
        wallet.addTransaction(new Income(new BigDecimal("0.20"), month.atDay(1), salary));
        wallet.addTransaction(new Expense(new BigDecimal("0.30"), month.atDay(2), food));
        assertEquals(Money.ZERO, wallet.getBalance());
        wallet.addTransaction(new Expense(BigDecimal.ONE, month.atDay(3), food));
        assertEquals(new BigDecimal("-1.00"), wallet.getBalance());
        assertTrue(wallet.getAlerts(month).stream().anyMatch(s -> s.contains("отрицательный")));
    }

    @Test
    void categoryNamesIgnoreCaseAndSurroundingSpaces() {
        assertSame(food, wallet.getCategory("  еДА  "));
        assertThrows(IllegalArgumentException.class, () -> wallet.createCategory(" ЕДА ", Category.Type.INCOME));
        assertEquals(2, wallet.getCategories().size());
    }

    @Test
    void rejectsInvalidOrUnknownCategories() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.createCategory(" ", Category.Type.EXPENSE)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.createCategory("Транспорт", null)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.getCategory(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.getCategory("Нет")));
    }

    @Test
    void rejectsTransactionWithCategoryFromAnotherWalletEvenWithSameName() {
        Wallet another = new Wallet();
        Category foreign = another.createCategory("Еда", Category.Type.EXPENSE);
        assertThrows(IllegalArgumentException.class,
                () -> wallet.addTransaction(new Expense(BigDecimal.ONE, month.atDay(1), foreign)));
        assertThrows(IllegalArgumentException.class, () -> wallet.addTransaction(null));
        assertTrue(wallet.getTransactions().isEmpty());
    }

    @Test
    void collectionsAreUnmodifiableSnapshots() {
        var snapshot = wallet.getTransactions();
        wallet.addTransaction(new Income(BigDecimal.TEN, month.atDay(1), salary));
        assertTrue(snapshot.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> wallet.getTransactions().clear());
        assertThrows(UnsupportedOperationException.class, () -> wallet.getCategories().clear());
        assertEquals(1, wallet.getTransactions().size());
    }

    @Test
    void budgetCountsOnlyMatchingCategoryAndMonthAcrossYearBoundary() {
        Category transport = wallet.createCategory("Транспорт", Category.Type.EXPENSE);
        wallet.setBudget("Еда", month, new BigDecimal("100"));
        wallet.addTransaction(new Expense(new BigDecimal("30"), month.atDay(1), food));
        wallet.addTransaction(new Expense(new BigDecimal("10"), month.atEndOfMonth(), food));
        wallet.addTransaction(new Expense(new BigDecimal("80"), month.minusMonths(1).atEndOfMonth(), food));
        wallet.addTransaction(new Expense(new BigDecimal("90"), month.plusMonths(1).atDay(1), food));
        wallet.addTransaction(new Expense(new BigDecimal("15"), month.atDay(2), transport));
        wallet.addTransaction(new Income(new BigDecimal("1000"), month.atDay(1), salary));
        Wallet.BudgetStatus status = wallet.getBudgetStatuses(month).get(0);
        assertEquals(new BigDecimal("40.00"), status.spent());
        assertEquals(new BigDecimal("60.00"), status.remaining());
        assertFalse(status.exceeded());
        assertTrue(wallet.getBudgetStatuses(month.plusMonths(1)).isEmpty());
    }

    @Test
    void equalityWithLimitDoesNotTriggerAlertButExceedingDoes() {
        wallet.addTransaction(new Income(new BigDecimal("1000"), month.atDay(1), salary));
        wallet.setBudget("Еда", month, new BigDecimal("100"));
        wallet.addTransaction(new Expense(new BigDecimal("100"), month.atDay(2), food));
        assertTrue(wallet.getAlerts(month).isEmpty());
        wallet.addTransaction(new Expense(new BigDecimal("0.01"), month.atDay(3), food));
        assertTrue(wallet.getBudgetStatuses(month).get(0).exceeded());
        assertEquals(1, wallet.getAlerts(month).size());
        assertTrue(wallet.getAlerts(month).get(0).contains("0.01 RUB"));
        assertEquals(3, wallet.getTransactions().size());
    }

    @Test
    void budgetCanBeSetAfterExpensesAndUpdatedWithoutChangingAnotherMonth() {
        wallet.addTransaction(new Expense(new BigDecimal("50"), month.atDay(1), food));
        wallet.setBudget("Еда", month, new BigDecimal("40"));
        wallet.setBudget("Еда", month.plusMonths(1), new BigDecimal("200"));
        assertTrue(wallet.getBudgetStatuses(month).get(0).exceeded());
        wallet.setBudget("Еда", month, new BigDecimal("60"));
        assertEquals(1, wallet.getBudgetStatuses(month).size());
        assertEquals(new BigDecimal("10.00"), wallet.getBudgetStatuses(month).get(0).remaining());
        assertEquals(new BigDecimal("200.00"), wallet.getBudgetStatuses(month.plusMonths(1)).get(0).limit());
    }

    @Test
    void invalidBudgetDoesNotOverwriteExistingLimit() {
        wallet.setBudget("Еда", month, BigDecimal.TEN);
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.setBudget("Еда", month, BigDecimal.ZERO)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.setBudget("Зарплата", month, BigDecimal.TEN)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.setBudget("Нет", month, BigDecimal.TEN)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.setBudget("Еда", null, BigDecimal.TEN)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.getBudgetStatuses(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> wallet.getAlerts(null)));
        assertEquals(new BigDecimal("10.00"), wallet.getBudgetStatuses(month).get(0).limit());
    }

    @Test
    void emptyWalletHasZeroBalanceAndNoAlerts() {
        assertEquals(Money.ZERO, wallet.getBalance());
        assertTrue(wallet.getAlerts(month).isEmpty());
        assertTrue(wallet.getBudgetStatuses(month).isEmpty());
    }
}
