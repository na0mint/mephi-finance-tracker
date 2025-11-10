package cli;

import exception.FinanceAppException;
import model.TransactionType;
import model.User;
import service.AuthService;
import service.PersistenceService;
import service.WalletService;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Cli {

    private final Scanner scanner = new Scanner(System.in);
    private final AuthService authService = new AuthService();
    private final PersistenceService persistenceService = new PersistenceService();
    private final WalletService walletService = new WalletService(persistenceService, authService);

    public Cli() {
        authService.loadUsers();
    }

    public void start() {
        println("Приложение для учёта расходов и доходов");
        println("Введите «help», чтобы посмотреть список доступных команд.");
        UserSession session = new UserSession();

        while (true) {
            System.out.print(session.user == null ? "> " : session.getLogin() + "> ");
            var line = scanner.hasNextLine() ? scanner.nextLine().trim() : null;
            if (line == null) break;
            if (line.isEmpty()) continue;

            var parts = splitArgs(line);
            var cmd = parts.get(0).toLowerCase(Locale.ROOT);

            try {
                switch (cmd) {
                    case "help" -> printHelp();

                    case "register" -> {
                        if (parts.size() < 3) {
                            println("Пример: register <логин> <пароль>");
                            break;
                        }
                        var login = parts.get(1);
                        var pass = parts.get(2);
                        authService.register(login, pass);
                        persistenceService.saveAll(authService, walletService);
                        println("Пользователь зарегистрирован: " + login);
                    }

                    case "login" -> {
                        if (parts.size() < 3) {
                            println("Использование: login <логин> <пароль>");
                            break;
                        }
                        var login = parts.get(1);
                        var pass = parts.get(2);
                        var user = authService.authenticate(login, pass);
                        session.login(user);
                        walletService.loadWalletForUser(user.getLogin());
                        println("Вы вошли как: " + login);
                    }

                    case "logout" -> {
                        session.logout();
                        println("Вы вышли из системы.");
                    }

                    case "create-category" -> {
                        if (!session.requireLogin()) break;
                        if (parts.size() < 2) {
                            println("Использование: create-category <название>");
                            break;
                        }
                        var cat = parts.get(1);
                        walletService.createCategory(session.getLogin(), cat);
                        println("Категория создана: " + cat);
                    }

                    case "set-budget" -> {
                        if (!session.requireLogin()) break;
                        if (parts.size() < 3) {
                            println("Использование: set-budget <категория> <сумма>");
                            break;
                        }
                        var cat = parts.get(1);
                        var amount = parseDouble(parts.get(2));
                        if (amount == null) { println("Некорректная сумма."); break; }
                        walletService.setBudget(session.getLogin(), cat, amount);
                        println("Бюджет установлен: " + cat + " = " + format(amount));
                    }

                    case "add-income" -> {
                        if (!session.requireLogin()) break;
                        if (parts.size() < 3) {
                            println("Использование: add-income <сумма> <категория> [описание]");
                            break;
                        }
                        var amount = parseDouble(parts.get(1));
                        if (amount == null) { println("Некорректная сумма."); break; }
                        var cat = parts.get(2);
                        var desc = parts.size() >= 4 ? parts.get(3) : "";
                        walletService.addTransaction(session.getLogin(), TransactionType.INCOME, amount, cat, desc);
                        persistenceService.saveAll(authService, walletService);
                        println("Доход добавлен.");
                    }

                    case "add-expense" -> {
                        if (!session.requireLogin()) break;
                        if (parts.size() < 3) {
                            println("Использование: add-expense <сумма> <категория> [описание]");
                            break;
                        }
                        var amount = parseDouble(parts.get(1));
                        if (amount == null) { println("Некорректная сумма."); break; }
                        var cat = parts.get(2);
                        var desc = parts.size() >= 4 ? parts.get(3) : "";
                        walletService.addTransaction(session.getLogin(), TransactionType.EXPENSE, amount, cat, desc);
                        persistenceService.saveAll(authService, walletService);
                        println("Расход добавлен.");
                    }

                    case "transfer" -> {
                        if (!session.requireLogin()) break;
                        if (parts.size() < 3) {
                            println("Использование: transfer <кому> <сумма> [описание]");
                            break;
                        }
                        var to = parts.get(1);
                        var amount = parseDouble(parts.get(2));
                        if (amount == null) { println("Некорректная сумма."); break; }
                        var desc = parts.size() >= 4 ? parts.get(3) : "";
                        walletService.transfer(session.getLogin(), to, amount, desc);
                        persistenceService.saveAll(authService, walletService);
                        println("Перевод выполнен пользователю " + to);
                    }

                    case "show-summary" -> {
                        if (!session.requireLogin()) break;
                        println(walletService.buildSummary(session.getLogin()));
                    }

                    case "show-category" -> {
                        if (!session.requireLogin()) break;
                        if (parts.size() < 2) {
                            println("Использование: show-category <категория...>");
                            break;
                        }
                        var cats = parts.subList(1, parts.size());
                        var map = walletService.sumByCategories(session.getLogin(), cats);
                        if (map.isEmpty()) println("Категории не найдены.");
                        else {
                            println("Сводка по категориям:");
                            map.forEach((c, s) -> println("  " + c + ": " + format(s)));
                        }
                    }

                    case "export" -> {
                        if (!session.requireLogin()) break;
                        if (parts.size() < 2) {
                            println("Использование: export <путь_к_файлу>");
                            break;
                        }
                        var path = parts.get(1);
                        var report = walletService.buildSummary(session.getLogin());
                        Files.writeString(new File(path).toPath(), report);
                        println("Отчёт сохранён в файл: " + path);
                    }

                    case "save" -> {
                        persistenceService.saveAll(authService, walletService);
                        println("Все данные сохранены.");
                    }

                    case "exit" -> {
                        println("Сохранение данных и выход...");
                        persistenceService.saveAll(authService, walletService);
                        println("До свидания!");
                        return;
                    }

                    default -> println("Неизвестная команда. Введите «help» для списка.");
                }

            } catch (FinanceAppException e) {
                println("Ошибка: " + e.getMessage());
            } catch (Exception e) {
                println("Неожиданная ошибка: " + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }

    private void printHelp() {
        println("""
                Команды:
                  register <логин> <пароль>         — регистрация пользователя
                  login <логин> <пароль>            — вход в систему
                  logout                            — выход
                  create-category <название>         — создать категорию
                  set-budget <категория> <сумма>    — установить бюджет
                  add-income <сумма> <категория> [описание] — добавить доход
                  add-expense <сумма> <категория> [описание] — добавить расход
                  transfer <логин> <сумма> [описание] — перевести средства
                  show-summary                      — показать сводку
                  show-category <категория...>       — показать категории
                  export <файл>                     — экспорт отчёта
                  save                              — сохранить данные
                  exit                              — сохранить и выйти
                  help                              — показать справку
                """);
    }

    private List<String> splitArgs(String line) {
        List<String> parts = new java.util.ArrayList<>();
        var sb = new StringBuilder();
        boolean inQuote = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }
            if (Character.isWhitespace(c) && !inQuote) {
                if (!sb.isEmpty()) {
                    parts.add(sb.toString());
                    sb.setLength(0);
                }
            } else sb.append(c);
        }
        if (!sb.isEmpty()) parts.add(sb.toString());
        return parts;
    }

    private Double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private void println(Object o) { System.out.println(o); }

    private String format(double d) { return String.format(java.util.Locale.forLanguageTag("ru"), "%.2f", d); }

    private static class UserSession {
        private User user;

        boolean requireLogin() {
            if (user == null) {
                System.out.println("Сначала войдите в систему (команда: login).");
                return false;
            }
            return true;
        }

        void login(User u) { this.user = u; }
        void logout() { this.user = null; }
        String getLogin() { return user.getLogin(); }
    }
}


