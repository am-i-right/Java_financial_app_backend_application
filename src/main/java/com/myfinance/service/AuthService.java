package com.myfinance.service;

import com.myfinance.model.User;
import java.util.HashMap;
import java.util.Map;

/** Одна активная сессия в консольном приложении; пользователи живут до завершения процесса. */
public final class AuthService {
    private final Map<String, User> users = new HashMap<>();
    private User currentUser;

    public void register(String login, String password) {
        String normalizedLogin = normalize(login);
        if (users.containsKey(normalizedLogin)) {
            throw new IllegalArgumentException("Этот логин уже занят.");
        }
        users.put(normalizedLogin, new User(normalizedLogin, password));
    }

    public void login(String login, String password) {
        if (currentUser != null) {
            throw new IllegalStateException("Сначала выйдите из текущей учётной записи.");
        }
        User user = users.get(normalize(login));
        if (user == null || !user.matchesPassword(password)) {
            throw new IllegalArgumentException("Неверный логин или пароль.");
        }
        currentUser = user;
    }

    public void logout() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("Сначала войдите в учётную запись.");
        }
        return currentUser;
    }

    private static String normalize(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин не должен быть пустым.");
        }
        return login.strip();
    }
}
