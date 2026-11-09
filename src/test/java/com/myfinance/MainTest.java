package com.myfinance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

class MainTest {
    @Test
    void fullConsoleScenarioRegistersUserAndShowsBudgetAlert() {
        String text = execute("""
                1
                alice
                password1
                2
                alice
                password1
                1
                Зарплата
                1
                1
                Еда
                2
                2
                Зарплата
                1000
                2026-11-01
                4
                Еда
                2026-11
                100
                3
                Еда
                120,50
                2026-11-09
                5
                6
                7
                2026-11
                8
                10
                9
                0
                """);
        assertTrue(text.contains("Регистрация выполнена"));
        assertTrue(text.contains("Баланс: 879.50 RUB"));
        assertTrue(text.contains("Превышен бюджет категории «Еда» за 2026-11 на 20.50 RUB"));
        assertTrue(text.contains("Расходы: 120.50 RUB"));
        assertTrue(text.contains("2026-11-09 | Еда | -120.50 RUB"));
        assertTrue(text.contains("Вы вышли из аккаунта"));
        assertFalse(text.contains("Ошибка:"));
    }

    @Test
    void badAmountAndDateReturnToMenuWithoutAddingTransactions() {
        String text = execute(loginCommands() + """
                1
                Еда
                2
                3
                Еда
                нечисло
                3
                Еда
                10
                2026-02-30
                3
                Еда
                -10
                2026-11-09
                6
                0
                """);
        assertTrue(text.contains("введите сумму числом"));
        assertTrue(text.contains("используйте существующую дату"));
        assertTrue(text.contains("Сумма должна быть больше нуля"));
        assertTrue(text.contains("Операций пока нет"));
    }

    @Test
    void unknownCommandsAndFailedLoginDoNotCrashMenu() {
        String text = execute("unknown\n2\nalice\nwrongpass\n0\n");
        assertTrue(text.contains("Неизвестная команда"));
        assertTrue(text.contains("Неверный логин или пароль"));
        assertTrue(text.contains("Работа завершена"));
    }

    @Test
    void invalidCategoryTypeAndMonthAreReported() {
        String text = execute(loginCommands() + "1\nЕда\n3\n7\n2026-13\n0\n");
        assertTrue(text.contains("Тип должен быть 1 или 2"));
        assertTrue(text.contains("используйте существующую дату"));
    }

    @Test
    void eofInMiddleOfCommandEndsCleanly() {
        String text = execute("1\nalice\n");
        assertTrue(text.contains("Ввод завершён"));
        assertTrue(text.contains("Работа завершена"));
    }

    @Test
    void passwordSpacesArePreservedInConsole() {
        String text = execute("1\nalice\n password1 \n2\nalice\n password1 \n0\n");
        assertTrue(text.contains("Вход выполнен"));
        assertFalse(text.contains("Ошибка:"));
    }

    @Test
    void blankDateAndMonthUseCurrentPeriod() {
        String text = execute(loginCommands() + "1\nЗарплата\n1\n2\nЗарплата\n10\n\n7\n\n0\n");
        assertTrue(text.contains("Доходы: 10.00 RUB"));
        assertFalse(text.contains("Ошибка:"));
    }

    private static String loginCommands() {
        return "1\nalice\npassword1\n2\nalice\npassword1\n";
    }

    private static String execute(String commands) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner input = new Scanner(commands);
             PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            Main.run(input, out);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
