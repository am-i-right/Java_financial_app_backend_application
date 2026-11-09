package com.myfinance.service;

import static org.junit.jupiter.api.Assertions.*;

import com.myfinance.model.Category;
import com.myfinance.model.Expense;
import com.myfinance.model.Income;
import com.myfinance.model.Wallet;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ReportGeneratorTest {
    private final Report report = new ReportGenerator();
    private final YearMonth month = YearMonth.of(2026, 11);

    @Test
    void reportsMonthlyTotalsCategoriesBudgetsAndAllTimeBalance() {
        Wallet wallet = new Wallet();
        Category salary = wallet.createCategory("Зарплата", Category.Type.INCOME);
        Category food = wallet.createCategory("Еда", Category.Type.EXPENSE);
        wallet.addTransaction(new Income(new BigDecimal("1000"), month.atDay(1), salary));
        wallet.addTransaction(new Expense(new BigDecimal("120"), month.atDay(2), food));
        wallet.addTransaction(new Expense(new BigDecimal("30"), month.atDay(3), food));
        wallet.addTransaction(new Income(new BigDecimal("300"), month.minusMonths(1).atDay(1), salary));
        wallet.setBudget("Еда", month, new BigDecimal("100"));
        String text = report.generate(wallet, month);
        assertAll(
                () -> assertTrue(text.contains("Доходы: 1000.00 RUB")),
                () -> assertTrue(text.contains("Расходы: 150.00 RUB")),
                () -> assertTrue(text.contains("Итог месяца: 850.00 RUB")),
                () -> assertTrue(text.contains("Баланс за всё время: 1150.00 RUB")),
                () -> assertTrue(text.contains("- Еда: 150.00 RUB")),
                () -> assertTrue(text.contains("остаток -50.00 RUB")),
                () -> assertTrue(text.contains("Превышен бюджет")));
    }

    @Test
    void supportsEmptyWallet() {
        String text = report.generate(new Wallet(), month);
        assertTrue(text.contains("Доходы: 0.00 RUB"));
        assertTrue(text.contains("Расходы: 0.00 RUB"));
        assertTrue(text.contains("Операций нет."));
        assertTrue(text.contains("Не заданы."));
    }

    @Test
    void budgetWithoutExpensesShowsEntireLimitAsRemaining() {
        Wallet wallet = new Wallet();
        wallet.createCategory("Еда", Category.Type.EXPENSE);
        wallet.setBudget("Еда", month, BigDecimal.TEN);
        String text = report.generate(wallet, month);
        assertTrue(text.contains("лимит 10.00, потрачено 0.00, остаток 10.00 RUB"));
        assertFalse(text.contains("Превышен бюджет"));
    }

    @Test
    void rejectsMissingReportArguments() {
        assertThrows(IllegalArgumentException.class, () -> report.generate(null, month));
        assertThrows(IllegalArgumentException.class, () -> report.generate(new Wallet(), null));
    }
}
