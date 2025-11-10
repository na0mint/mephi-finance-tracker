package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet extends BaseEntity implements Serializable {
    private final String ownerLogin;
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<String, Category> categories = new HashMap<>();

    public Wallet(String ownerLogin) { this.ownerLogin = ownerLogin; }

    public String getOwnerLogin() { return ownerLogin; }
    public List<Transaction> getTransactions() { return transactions; }
    public Map<String, Category> getCategories() { return categories; }
}

