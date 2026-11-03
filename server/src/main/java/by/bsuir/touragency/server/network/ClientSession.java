package by.bsuir.touragency.server.network;

import by.bsuir.touragency.common.model.User;

/**
 * Состояние подключения одного клиента: кто авторизован.
 * Каждый поток-обработчик владеет своей сессией.
 */
public class ClientSession {

    private final String remoteAddress;
    private User user;

    public ClientSession(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public boolean isAuthenticated() {
        return user != null;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void clear() {
        this.user = null;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public String toString() {
        return remoteAddress + (user == null ? " [гость]" : " [" + user.getLogin() + "]");
    }
}
