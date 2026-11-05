package by.bsuir.touragency.server.service;

import by.bsuir.touragency.common.exception.AuthException;
import by.bsuir.touragency.server.dao.AuditDao;
import by.bsuir.touragency.server.dao.UserDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Проверка временной блокировки входа после пяти неудачных попыток. */
class LoginLockoutTest {

    /** Заглушка DAO: пользователей нет, журнал ничего не пишет. */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> method.getReturnType() == Optional.class ? Optional.empty()
                        : method.getReturnType() == boolean.class ? false : null);
    }

    private static String key(AuthException e) {
        return e.getMessageKey();
    }

    @Test
    @DisplayName("После 5 неудачных попыток вход блокируется на 5 минут")
    void locksAfterFiveFailures() {
        AuthService auth = new AuthService(stub(UserDao.class), stub(AuditDao.class));
        Instant start = Instant.parse("2027-01-01T10:00:00Z");
        auth.setClock(Clock.fixed(start, ZoneOffset.UTC));

        for (int i = 1; i < AuthService.MAX_FAILED_ATTEMPTS; i++) {
            assertEquals("auth.invalid", key(assertThrows(AuthException.class, () -> auth.login("ivan", "bad"))));
        }
        assertEquals("auth.locked", key(assertThrows(AuthException.class, () -> auth.login("ivan", "bad"))));
        // пока блокировка действует, даже проверка пароля не выполняется
        assertEquals("auth.locked", key(assertThrows(AuthException.class, () -> auth.login("IVAN", "any"))));

        auth.setClock(Clock.fixed(start.plus(Duration.ofMinutes(5)).plusSeconds(1), ZoneOffset.UTC));
        assertEquals("auth.invalid", key(assertThrows(AuthException.class, () -> auth.login("ivan", "bad"))));
    }

    @Test
    @DisplayName("Блокировка одного логина не мешает входу под другим")
    void lockIsPerLogin() {
        AuthService auth = new AuthService(stub(UserDao.class), stub(AuditDao.class));
        for (int i = 0; i < AuthService.MAX_FAILED_ATTEMPTS; i++) {
            assertThrows(AuthException.class, () -> auth.login("ivan", "bad"));
        }
        assertEquals("auth.invalid", key(assertThrows(AuthException.class, () -> auth.login("anna", "bad"))));
    }
}
