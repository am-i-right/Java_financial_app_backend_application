package com.myfinance.service;

import static org.junit.jupiter.api.Assertions.*;

import com.myfinance.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthServiceTest {
    private final AuthService auth = new AuthService();

    @Test
    void registrationLoginAndLogout() {
        auth.register(" alice ", "password1");
        assertFalse(auth.isLoggedIn());
        auth.login("alice", "password1");
        assertEquals("alice", auth.getCurrentUser().getLogin());
        auth.logout();
        assertFalse(auth.isLoggedIn());
        assertThrows(IllegalStateException.class, auth::getCurrentUser);
        auth.login(" alice ", "password1");
        assertTrue(auth.isLoggedIn());
    }

    @Test
    void duplicateRegistrationDoesNotReplacePassword() {
        auth.register("alice", "password1");
        assertThrows(IllegalArgumentException.class, () -> auth.register(" alice ", "password2"));
        auth.login("alice", "password1");
    }

    @Test
    void invalidCredentialsNeverCreateSession() {
        auth.register("alice", "password1");
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> auth.login("alice", "wrongpass")),
                () -> assertThrows(IllegalArgumentException.class, () -> auth.login("unknown", "password1")),
                () -> assertThrows(IllegalArgumentException.class, () -> auth.login("alice", null)));
        assertFalse(auth.isLoggedIn());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "1234567", "        "})
    void rejectsInvalidPassword(String password) {
        assertThrows(IllegalArgumentException.class, () -> auth.register("alice", password));
        assertFalse(auth.isLoggedIn());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankLogin(String login) {
        assertThrows(IllegalArgumentException.class, () -> auth.register(login, "password1"));
    }

    @Test
    void passwordIsCaseSensitiveAndSpacesAreSignificant() {
        User user = new User("alice", " secretPass ");
        assertTrue(user.matchesPassword(" secretPass "));
        assertFalse(user.matchesPassword("secretPass"));
        assertFalse(user.matchesPassword(" secretpass "));
        assertFalse(user.matchesPassword(null));
    }

    @Test
    void mustLogoutBeforeSwitchingUsers() {
        auth.register("alice", "password1");
        auth.register("bob", "password2");
        auth.login("alice", "password1");
        assertThrows(IllegalStateException.class, () -> auth.login("bob", "password2"));
        assertEquals("alice", auth.getCurrentUser().getLogin());
    }
}
