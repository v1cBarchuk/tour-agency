package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.User;

/**
 * Пользователь вместе с хешем пароля и солью. Используется только на сервере.
 */
public record UserCredentials(User user, String passwordHash, String salt) {
}
