package by.bsuir.touragency.common.model;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Пользователь системы. Хеш пароля и соль на клиент не передаются.
 */
public class User extends Entity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String login;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private boolean blocked;
    private Lang language = Lang.RU;
    private LocalDateTime createdAt;
    private int bookingsCount;        // для списка пользователей
    private LocalDateTime lastLogin;
    private java.time.LocalDate birthDate;      // личный кабинет
    private String homeCity;                    // город вылета
    private boolean notifyPriceDrop = true;     // уведомлять о снижении цен на избранное

    public java.time.LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(java.time.LocalDate birthDate) { this.birthDate = birthDate; }
    public String getHomeCity() { return homeCity; }
    public void setHomeCity(String homeCity) { this.homeCity = homeCity; }
    public boolean isNotifyPriceDrop() { return notifyPriceDrop; }
    public void setNotifyPriceDrop(boolean notifyPriceDrop) { this.notifyPriceDrop = notifyPriceDrop; }

    public User() {
    }

    public User(Integer id, String login, String fullName, String email, Role role) {
        super(id);
        this.login = login;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    @Override
    public String getDisplayName() {
        return fullName + " (" + login + ")";
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public Lang getLanguage() { return language; }
    public void setLanguage(Lang language) { this.language = language; }

    public int getBookingsCount() { return bookingsCount; }
    public void setBookingsCount(int v) { bookingsCount = v; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime v) { lastLogin = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
