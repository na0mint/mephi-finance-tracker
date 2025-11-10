package service;

import exception.FinanceAppException;
import model.Category;
import model.Transaction;
import model.TransactionType;
import model.Wallet;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Сервис управления кошельками: транзакции, категории, бюджеты, переводы и отчёты.
 */
public class WalletService {

    private final PersistenceService persistence;
    private final AuthService authService;
    private final Map<String, Wallet> wallets = new HashMap<>();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public WalletService(PersistenceService persistence, AuthService authService) {
        this.persistence = persistence;
        this.authService = authService;
    }

    public void loadWalletForUser(String login) {
        wallets.put(login, persistence.loadWallet(login));
    }

    public Wallet getWalletIfLoaded(String login) {
        return wallets.get(login);
    }

    private Wallet walletFor(String login) {
        return wallets.computeIfAbsent(login, persistence::loadWallet);
    }

    public void createCategory(String login, String category) {
        var w = walletFor(login);

        if (category == null || category.isBlank()) throw new FinanceAppException("Название категории не может быть пустым.");
        if (w.getCategories().containsKey(category)) throw new FinanceAppException("Категория уже существует.");

        w.getCategories().put(category, new Category(category));
    }

    public void setBudget(String login, String category, double amount) {
        if (amount < 0) throw new FinanceAppException("Бюджет не может быть отрицательным.");

        var w = walletFor(login);
        var cat = w.getCategories().computeIfAbsent(category, Category::new);

        cat.setBudget(amount);
    }

    public void addTransaction(String login, TransactionType type, double amount, String category, String description) {
        if (amount <= 0) throw new FinanceAppException("Сумма должна быть положительной.");
        var w = walletFor(login);

        if (type == TransactionType.EXPENSE && !w.getCategories().containsKey(category)) {
            throw new FinanceAppException("Категория не найдена: " + category);
        }

        if (type == TransactionType.INCOME && !w.getCategories().containsKey(category)) {
            w.getCategories().put(category, new Category(category));
        }

        var t = new Transaction(type, amount, category, description == null ? "" : description, java.time.LocalDateTime.now());
        w.getTransactions().add(t);

        if (type == TransactionType.EXPENSE) {
            Category c = w.getCategories().get(category);
            if (c != null && c.getBudget() > 0) {
                double spent = sumByCategory(w, category, TransactionType.EXPENSE);
                if (spent > c.getBudget()) {
                    System.out.println("[ВНИМАНИЕ] Превышен бюджет категории '" + category + "'. Бюджет: " + format(c.getBudget()) + ", Расход: " + format(spent));
                }
            }
        }

        double totalIncome = sumTotal(w, TransactionType.INCOME);
        double totalExpense = sumTotal(w, TransactionType.EXPENSE);

        if (totalExpense > totalIncome) {
            System.out.println("[ВНИМАНИЕ] Общие расходы превысили доходы. Доход: " + format(totalIncome) + ", Расход: " + format(totalExpense));
        }
    }

    public void transfer(String fromLogin, String toLogin, double amount, String description) {
        if (amount <= 0) throw new FinanceAppException("Сумма должна быть положительной.");
        if (fromLogin.equals(toLogin)) throw new FinanceAppException("Нельзя переводить самому себе.");
        if (!authService.userExists(toLogin)) throw new FinanceAppException("Пользователь-получатель не найден: " + toLogin);

        var fromW = walletFor(fromLogin);
        var toW = walletFor(toLogin);

        String catFrom = "transfer-out";
        String catTo = "transfer-in";

        fromW.getCategories().computeIfAbsent(catFrom, Category::new);
        toW.getCategories().computeIfAbsent(catTo, Category::new);

        var tOut = new Transaction(TransactionType.EXPENSE, amount, catFrom, "Перевод: " + toLogin + (description == null || description.isBlank() ? "" : " — " + description), java.time.LocalDateTime.now());
        var tIn = new Transaction(TransactionType.INCOME, amount, catTo, "Перевод от: " + fromLogin + (description == null || description.isBlank() ? "" : " — " + description), java.time.LocalDateTime.now());

        fromW.getTransactions().add(tOut);
        toW.getTransactions().add(tIn);

        var budget = fromW.getCategories().get(catFrom);
        if (budget != null && budget.getBudget() > 0) {
            double spent = sumByCategory(fromW, catFrom, TransactionType.EXPENSE);
            if (spent > budget.getBudget()) {
                System.out.println("[ВНИМАНИЕ] Превышен бюджет переводов для пользователя " + fromLogin);
            }
        }

        double totalIncome = sumTotal(fromW, TransactionType.INCOME);
        double totalExpense = sumTotal(fromW, TransactionType.EXPENSE);

        if (totalExpense > totalIncome) {
            System.out.println("[ВНИМАНИЕ] У пользователя " + fromLogin + " расходы превысили доходы.");
        }
    }

    public String buildSummary(String login) {
        var w = walletFor(login);
        var sb = new StringBuilder();
        sb.append("=== Сводка пользователя: ").append(login).append(" ===\n");

        var inc = sumTotal(w, TransactionType.INCOME);
        var exp = sumTotal(w, TransactionType.EXPENSE);
        sb.append("Общий доход: ").append(format(inc)).append("\n");
        sb.append("Общий расход: ").append(format(exp)).append("\n\n");

        sb.append("Доходы по категориям:\n");
        var incMap = groupByCategory(w, TransactionType.INCOME);
        if (incMap.isEmpty()) sb.append("  (нет)\n");
        else incMap.forEach((c, s) -> sb.append("  ").append(c).append(": ").append(format(s)).append("\n"));

        sb.append("\nРасходы по категориям:\n");
        var expMap = groupByCategory(w, TransactionType.EXPENSE);
        if (expMap.isEmpty()) sb.append("  (нет)\n");
        else expMap.forEach((c, s) -> sb.append("  ").append(c).append(": ").append(format(s)).append("\n"));

        sb.append("\nБюджеты:\n");
        if (w.getCategories().isEmpty()) sb.append("  (нет)\n");
        else {
            var sorted = w.getCategories().values().stream().sorted(Comparator.comparing(Category::getName))
                    .toList();
            for (var cat : sorted) {
                var spent = sumByCategory(w, cat.getName(), TransactionType.EXPENSE);
                var left = cat.getBudget() - spent;
                sb.append("  ").append(cat.getName()).append(": ").append(format(cat.getBudget()))
                        .append(", Остаток: ").append(format(left)).append("\n");
            }
        }

        sb.append("\nТранзакции:\n");
        if (w.getTransactions().isEmpty()) sb.append("  (нет)\n");
        else {
            for (var t : w.getTransactions()) {
                sb.append("  [").append(t.time().format(TIME_FORMAT)).append("] ")
                        .append(t.type()).append(" ").append(format(t.amount()))
                        .append(" (").append(t.category()).append(") ").append(t.description()).append("\n");
            }
        }

        return sb.toString();
    }

    public Map<String, Double> sumByCategories(String login, List<String> categories) {
        var w = walletFor(login);
        Map<String, Double> out = new LinkedHashMap<>();

        for (var c : categories) {
            if (w.getCategories().containsKey(c)) {
                double sIn = sumByCategory(w, c, TransactionType.INCOME);
                double sEx = sumByCategory(w, c, TransactionType.EXPENSE);
                out.put(c, sIn - sEx);
            }
        }
        return out;
    }

    public String format(double d) {
        return String.format(java.util.Locale.forLanguageTag("ru"), "%.2f", d);
    }

    private double sumTotal(Wallet w, TransactionType type) {
        return w.getTransactions().stream().filter(t -> t.type() == type).mapToDouble(Transaction::amount).sum();
    }

    private double sumByCategory(Wallet w, String category, TransactionType type) {
        return w.getTransactions().stream()
                .filter(t -> t.type() == type && Objects.equals(t.category(), category))
                .mapToDouble(Transaction::amount).sum();
    }

    private Map<String, Double> groupByCategory(Wallet w, TransactionType type) {
        Map<String, Double> map = new TreeMap<>();

        w.getTransactions().stream()
                .filter(t -> t.type() == type)
                .forEach(t -> map.merge(t.category(), t.amount(), Double::sum));

        return map;
    }
}

