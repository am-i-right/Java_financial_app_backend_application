package com.myfinance.service;

import com.myfinance.model.Wallet;
import java.time.YearMonth;

/** Позволяет заменить текстовый отчёт другой реализацией без изменения FinanceManager. */
public interface Report {
    String generate(Wallet wallet, YearMonth month);
}
