package model;

import java.io.Serializable;

public class Category extends BaseEntity implements Serializable {
    private final String name;
    private double budget = 0.0;

    public Category(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Название категории не может быть пустым.");
        this.name = name;
    }

    public String getName() { return name; }
    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = Math.max(0.0, budget); }

    @Override
    public String toString() {
        return name + " (бюджет=" + budget + ")";
    }
}

