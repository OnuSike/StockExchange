package com.example.exchange.engine;

import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderBook {

    private final PriorityQueue<Order> bids;
    private final PriorityQueue<Order> asks;

    public OrderBook() {
        this.bids = new PriorityQueue<>((o1, o2) -> Double.compare(o2.getPrice(), o1.getPrice()));
        this.asks = new PriorityQueue<>(Comparator.comparingDouble(Order::getPrice));
    }

    public PriorityQueue<Order> getBids() { return bids; }
    public PriorityQueue<Order> getAsks() { return asks; }

    public void addOrder(Order order) {
        if (order.getOrderType() == OrderType.BUY) {
            bids.add(order);
        } else {
            asks.add(order);
        }
    }

    public void removeOrder(Order order) {
        if (order.getOrderType() == OrderType.BUY) {
            bids.remove(order);
        } else {
            asks.remove(order);
        }
    }
}
