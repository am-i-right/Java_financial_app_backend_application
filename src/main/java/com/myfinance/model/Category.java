package com.myfinance.model;

public final class Category {
    public enum Type { INCOME, EXPENSE }

    private final String name;
    private final Type type;

    public Category(String name, Type type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название категории не должно быть пустым.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Укажите тип категории.");
        }
        this.name = name.strip();
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }
}
