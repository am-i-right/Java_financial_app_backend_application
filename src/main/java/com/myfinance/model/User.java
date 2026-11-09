package com.myfinance.model;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class User {
    private static final int HASH_ITERATIONS = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String login;
    private final byte[] salt = new byte[16];
    private final byte[] passwordHash;
    private final Wallet wallet = new Wallet();

    public User(String login, String password) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин не должен быть пустым.");
        }
        if (password == null || password.length() < 8 || password.isBlank()) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 8 символов и не состоять из пробелов.");
        }
        this.login = login.strip();
        RANDOM.nextBytes(salt);
        this.passwordHash = hash(password);
    }

    public String getLogin() {
        return login;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public boolean matchesPassword(String password) {
        return password != null && MessageDigest.isEqual(passwordHash, hash(password));
    }

    private byte[] hash(String password) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Не удалось вычислить хеш пароля.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
