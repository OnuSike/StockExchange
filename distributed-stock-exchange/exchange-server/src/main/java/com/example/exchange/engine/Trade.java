package com.example.exchange.engine;

public class Trade {
    private final String stockSymbol;
    private final int quantity;
    private final double price;
    private final String buyerId;
    private final String sellerId;

    public Trade(String stockSymbol, int quantity, double price, String buyerId, String sellerId) {
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.price = price;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
    }

    @Override
    public String toString() {
        return String.format("[TRADE] %d %s @ $%.2f (Buyer: %s, Seller: %s)",
                quantity, stockSymbol, price, buyerId, sellerId);
    }

    public String getStockSymbol() { return stockSymbol; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getBuyerId() { return buyerId; }
    public String getSellerId() { return sellerId; }
}
