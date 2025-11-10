package service;


import exception.FinanceAppException;
import model.User;
import util.HashUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис авторизации
 */
public class AuthService {

    private final Map<String, String> users = new HashMap<>();
    private static final String USERS_FILE = "users.dat";

    public void register(String login, String password) {
        validateLoginAndPassword(login, password);
        if (users.containsKey(login)) throw new FinanceAppException("Пользователь с таким логином уже существует.");
        users.put(login, HashUtil.sha256(password));
        saveUsers();
    }

    public User authenticate(String login, String password) {
        validateLoginAndPassword(login, password);

        var stored = users.get(login);

        if (stored == null) throw new FinanceAppException("Пользователь не найден.");
        if (!stored.equals(HashUtil.sha256(password))) throw new FinanceAppException("Неверный пароль.");

        return new User(login);
    }

    public boolean userExists(String login) {
        return users.containsKey(login);
    }

    public java.util.Set<String> knownUsers() {
        return java.util.Set.copyOf(users.keySet());
    }

    public void loadUsers() {
        var f = new File(USERS_FILE);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object o = ois.readObject();
            if (o instanceof Map<?, ?> m) {
                users.clear();
                m.forEach((k, v) -> users.put(String.valueOf(k), String.valueOf(v)));
            }
        } catch (Exception e) {
            System.out.println("Не удалось загрузить пользователей: " + e.getMessage());
        }
    }

    public void saveUsers() {
        var f = new File(USERS_FILE);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(users);
        } catch (Exception e) {
            System.out.println("Не удалось сохранить пользователей: " + e.getMessage());
        }
    }

    private void validateLoginAndPassword(String login, String password) {
        if (login == null || login.isBlank()) throw new FinanceAppException("Логин не может быть пустым.");
        if (password == null || password.isBlank()) throw new FinanceAppException("Пароль не может быть пустым.");
        if (login.contains(File.separator) || login.contains("..")) throw new FinanceAppException("Неверный логин.");
    }
}
