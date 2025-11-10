package model;

public class User extends BaseEntity {
    private final String login;

    public User(String login) {
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Логин не может быть пустым.");
        this.login = login;
    }

    public String getLogin() { return login; }
}

