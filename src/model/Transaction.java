package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record Transaction(TransactionType type, double amount, String category, String description,
                          LocalDateTime time) implements Serializable {
    public Transaction(TransactionType type, double amount, String category, String description, LocalDateTime time) {
        this.type = type;
        this.amount = amount;
        this.category = category == null ? "" : category;
        this.description = description == null ? "" : description;
        this.time = time == null ? LocalDateTime.now() : time;
    }
}

