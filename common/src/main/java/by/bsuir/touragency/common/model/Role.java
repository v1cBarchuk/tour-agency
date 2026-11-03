package by.bsuir.touragency.common.model;

/**
 * Роли пользователей. Идентификаторы совпадают с таблицей roles.
 */
public enum Role {
    ADMIN(1, "Администратор"),
    MANAGER(2, "Менеджер"),
    CLIENT(3, "Клиент");

    private final int id;
    private final String title;

    Role(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public static Role fromId(int id) {
        for (Role role : values()) {
            if (role.id == id) {
                return role;
            }
        }
        throw new IllegalArgumentException("Неизвестная роль: " + id);
    }

    @Override
    public String toString() {
        return title;
    }
}
