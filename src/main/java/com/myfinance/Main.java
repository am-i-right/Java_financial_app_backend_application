package com.myfinance;

import com.myfinance.model.Category;
import com.myfinance.model.Transaction;
import com.myfinance.service.AuthService;
import com.myfinance.service.FinanceManager;
import com.myfinance.service.ReportGenerator;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in, StandardCharsets.UTF_8)) {
            run(input, new PrintStream(System.out, true, StandardCharsets.UTF_8));
        }
    }

    /** Потоки передаются извне, поэтому меню можно проверить без подмены System.in/out. */
    public static void run(Scanner input, PrintStream out) {
        AuthService auth = new AuthService();
        FinanceManager finance = new FinanceManager(auth);
        out.println("Личные финансы. Все данные хранятся в памяти до завершения программы.");
        try {
            while (true) {
                out.println(auth.isLoggedIn()
                        ? "\nПользователь: " + auth.getCurrentUser().getLogin()
                            + "\n1 Категория | 2 Доход | 3 Расход | 4 Бюджет | 5 Баланс"
                            + "\n6 Операции по дате | 7 Отчёт за месяц | 8 Категории"
                            + "\n9 Выйти из аккаунта | 10 Операции по сумме | 0 Завершить"
                        : "\n1 Регистрация | 2 Вход | 0 Завершить");
                String command = read(input, out, "> ");
                if (command.equals("0")) {
                    break;
                }
                try {
                    if (!auth.isLoggedIn()) {
                        switch (command) {
                            case "1" -> {
                                auth.register(read(input, out, "Логин: "), readPassword(input, out));
                                out.println("Регистрация выполнена. Теперь войдите.");
                            }
                            case "2" -> {
                                auth.login(read(input, out, "Логин: "), readPassword(input, out));
                                out.println("Вход выполнен.");
                            }
                            default -> out.println("Неизвестная команда.");
                        }
                    } else {
                        switch (command) {
                            case "1" -> {
                                String name = read(input, out, "Название категории: ");
                                String type = read(input, out, "Тип (1 — доход, 2 — расход): ");
                                Category.Type categoryType = switch (type) {
                                    case "1" -> Category.Type.INCOME;
                                    case "2" -> Category.Type.EXPENSE;
                                    default -> throw new IllegalArgumentException("Тип должен быть 1 или 2.");
                                };
                                finance.createCategory(name, categoryType);
                                out.println("Категория создана.");
                            }
                            case "2", "3" -> {
                                String category = read(input, out, "Категория: ");
                                BigDecimal amount = readAmount(input, out);
                                LocalDate date = readDate(input, out);
                                if (command.equals("2")) {
                                    finance.addIncome(category, amount, date);
                                } else {
                                    finance.addExpense(category, amount, date);
                                }
                                out.println("Операция добавлена.");
                                finance.getAlerts(YearMonth.from(date)).forEach(out::println);
                            }
                            case "4" -> {
                                String category = read(input, out, "Категория расходов: ");
                                YearMonth month = readMonth(input, out);
                                finance.setBudget(category, month, readAmount(input, out));
                                out.println("Бюджет установлен.");
                                finance.getAlerts(month).forEach(out::println);
                            }
                            case "5" -> out.println("Баланс: " + finance.getBalance().toPlainString() + " RUB");
                            case "6" -> printTransactions(finance.getTransactions(), out);
                            case "7" -> out.print(finance.generateReport(new ReportGenerator(), readMonth(input, out)));
                            case "8" -> {
                                List<Category> categories = finance.getCategories();
                                if (categories.isEmpty()) {
                                    out.println("Категорий пока нет.");
                                }
                                categories.forEach(category -> out.println(category.getName() + " — "
                                        + (category.getType() == Category.Type.INCOME ? "доход" : "расход")));
                            }
                            case "9" -> {
                                auth.logout();
                                out.println("Вы вышли из аккаунта.");
                            }
                            case "10" -> printTransactions(finance.getTransactionsByAmount(), out);
                            default -> out.println("Неизвестная команда.");
                        }
                    }
                } catch (DateTimeParseException e) {
                    out.println("Ошибка: используйте существующую дату ГГГГ-ММ-ДД или месяц ГГГГ-ММ.");
                } catch (NumberFormatException e) {
                    out.println("Ошибка: введите сумму числом, например 1250.50.");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    out.println("Ошибка: " + e.getMessage());
                }
            }
        } catch (EndOfInput e) {
            out.println("\nВвод завершён.");
        }
        out.println("Работа завершена. Данные удалены из памяти.");
    }

    private static void printTransactions(List<Transaction> transactions, PrintStream out) {
        if (transactions.isEmpty()) {
            out.println("Операций пока нет.");
        }
        for (Transaction transaction : transactions) {
            out.println(transaction.getDate() + " | " + transaction.getCategory().getName()
                    + " | " + transaction.getSignedAmount().toPlainString() + " RUB");
        }
    }

    private static String read(Scanner input, PrintStream out, String prompt) {
        out.print(prompt);
        out.flush();
        if (!input.hasNextLine()) {
            throw new EndOfInput();
        }
        return input.nextLine().strip();
    }

    private static String readPassword(Scanner input, PrintStream out) {
        // Один Scanner для терминала, IDE и тестов; пробелы в пароле не обрезаем.
        out.print("Пароль (минимум 8 символов; ввод виден): ");
        out.flush();
        if (!input.hasNextLine()) {
            throw new EndOfInput();
        }
        return input.nextLine();
    }

    private static BigDecimal readAmount(Scanner input, PrintStream out) {
        return new BigDecimal(read(input, out, "Сумма RUB: ").replace(',', '.'));
    }

    private static LocalDate readDate(Scanner input, PrintStream out) {
        String value = read(input, out, "Дата ГГГГ-ММ-ДД (Enter — сегодня): ");
        return value.isEmpty() ? LocalDate.now() : LocalDate.parse(value);
    }

    private static YearMonth readMonth(Scanner input, PrintStream out) {
        String value = read(input, out, "Месяц ГГГГ-ММ (Enter — текущий): ");
        return value.isEmpty() ? YearMonth.now() : YearMonth.parse(value);
    }

    private static final class EndOfInput extends RuntimeException {
    }
}
