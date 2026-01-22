package com.example.exchange.engine;

import java.util.UUID;

public class Alert {
    private final String id;
    private final String orderId;
    private final String stockSymbol;
    private final String sellerId;
    private final long createdAt;
    private volatile double price;
    private volatile int quantity;
    private volatile boolean claimed;

    public Alert(String orderId, String stockSymbol, double price, int quantity, String sellerId) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.stockSymbol = stockSymbol;
        this.price = price;
        this.quantity = quantity;
        this.sellerId = sellerId;
        this.createdAt = System.currentTimeMillis();
        this.claimed = false;
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getStockSymbol() { return stockSymbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getSellerId() { return sellerId; }
    public long getCreatedAt() { return createdAt; }
    public boolean isClaimed() { return claimed; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean claim() {
        if (claimed) return false;
        claimed = true;
        return true;
    }
}
