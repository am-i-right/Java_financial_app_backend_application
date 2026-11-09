package com.myfinance.service;

import com.myfinance.model.Category;
import com.myfinance.model.Money;
import com.myfinance.model.Transaction;
import com.myfinance.model.Wallet;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReportGenerator implements Report {
    @Override
    public String generate(Wallet wallet, YearMonth month) {
        if (wallet == null || month == null) {
            throw new IllegalArgumentException("Укажите кошелёк и месяц отчёта.");
        }
        BigDecimal income = Money.ZERO;
        BigDecimal expense = Money.ZERO;
        Map<Category, BigDecimal> totals = new LinkedHashMap<>();
        for (Transaction transaction : wallet.getTransactions()) {
            if (!YearMonth.from(transaction.getDate()).equals(month)) {
                continue;
            }
            if (transaction.getCategory().getType() == Category.Type.INCOME) {
                income = income.add(transaction.getAmount());
            } else {
                expense = expense.add(transaction.getAmount());
            }
            totals.merge(transaction.getCategory(), transaction.getAmount(), BigDecimal::add);
        }

        StringBuilder text = new StringBuilder("Отчёт за ").append(month).append('\n');
        text.append("Доходы: ").append(income.toPlainString()).append(" RUB\n");
        text.append("Расходы: ").append(expense.toPlainString()).append(" RUB\n");
        text.append("Итог месяца: ").append(income.subtract(expense).toPlainString()).append(" RUB\n");
        text.append("Баланс за всё время: ").append(wallet.getBalance().toPlainString()).append(" RUB\n");
        text.append("По категориям:\n");
        if (totals.isEmpty()) {
            text.append("  Операций нет.\n");
        }
        totals.forEach((category, total) -> text.append("  ")
                .append(category.getType() == Category.Type.INCOME ? "+ " : "- ")
                .append(category.getName()).append(": ").append(total.toPlainString()).append(" RUB\n"));

        text.append("Бюджеты:\n");
        var statuses = wallet.getBudgetStatuses(month);
        if (statuses.isEmpty()) {
            text.append("  Не заданы.\n");
        }
        for (Wallet.BudgetStatus status : statuses) {
            text.append("  ").append(status.category().getName())
                    .append(": лимит ").append(status.limit().toPlainString())
                    .append(", потрачено ").append(status.spent().toPlainString())
                    .append(", остаток ").append(status.remaining().toPlainString()).append(" RUB\n");
        }
        for (String alert : wallet.getAlerts(month)) {
            text.append("Внимание: ").append(alert).append('\n');
        }
        return text.toString();
    }
}
