package com.example.exchange.dto;

import com.example.exchange.engine.OrderType;

public class OrderRequest {
    private String stockSymbol;
    private OrderType orderType;
    private double price;
    private int quantity;
    private String traderId;

    public OrderRequest() {}

    public String getStockSymbol() { return stockSymbol; }
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getTraderId() { return traderId; }
    public void setTraderId(String traderId) { this.traderId = traderId; }
}
