package com.example.exchange.engine;

import java.util.Objects;
import java.util.UUID;

public class Order {
    private final String orderId;
    private final String traderId;
    private final String stockSymbol;
    private final OrderType orderType;
    private double price;
    private volatile int quantity;
    private long createdAt;

    public Order(String stockSymbol, OrderType orderType, double price, int quantity, String traderId) {
        this.orderId = UUID.randomUUID().toString();
        this.stockSymbol = stockSymbol;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
        this.traderId = traderId;
        this.createdAt = System.currentTimeMillis();
    }

    public String getOrderId() { return orderId; }
    public String getTraderId() { return traderId; }
    public String getStockSymbol() { return stockSymbol; }
    public OrderType getOrderType() { return orderType; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public long getCreatedAt() { return createdAt; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void resetTimestamp() {
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("%s %d %s @ $%.2f (ID: %s, Trader: %s)",
                orderType, quantity, stockSymbol, price, orderId.substring(0, 4), traderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return orderId.equals(order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
