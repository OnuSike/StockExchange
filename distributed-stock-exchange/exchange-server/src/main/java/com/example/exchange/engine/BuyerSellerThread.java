package com.example.exchange.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BuyerSellerThread extends Thread {

    private final StockExchange exchange;
    private final Random random = new Random();

    private final List<Order> myActiveOrders = new ArrayList<>();

    private static final Map<String, Double> STOCK_PRICES = Map.of(
            "AAPL", 150.0, "MSFT", 300.0, "GOOGL", 140.0,
            "INTC", 35.0, "AMD", 110.0, "NVDA", 450.0
    );
    private static final List<String> STOCK_SYMBOLS = new ArrayList<>(STOCK_PRICES.keySet());

    private static final long ORDER_TTL_MS = 5000;
    private static final double MODIFY_CHANCE = 0.3;

    public BuyerSellerThread(StockExchange exchange, String name) {
        super(name);
        this.exchange = exchange;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 40; i++) {

                if (!myActiveOrders.isEmpty() && random.nextDouble() < MODIFY_CHANCE) {
                    tryModifyOldOrder();
                } else {
                    submitNewOrder();
                }

                myActiveOrders.removeIf(order ->
                        order.getQuantity() == 0 ||
                                (System.currentTimeMillis() - order.getCreatedAt()) > (ORDER_TTL_MS * 4)
                );

                Thread.sleep(random.nextInt(400) + 100);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " was interrupted.");
        }
    }

    private void submitNewOrder() {
        String stock = STOCK_SYMBOLS.get(random.nextInt(STOCK_SYMBOLS.size()));
        double basePrice = STOCK_PRICES.get(stock);

        OrderType type = random.nextBoolean() ? OrderType.BUY : OrderType.SELL;
        int quantity = random.nextInt(91) + 10;

        double price = basePrice * (0.95 + random.nextDouble() * 0.10);
        price = Math.round(price * 100.0) / 100.0;

        Order order = new Order(stock, type, price, quantity, getName());
        System.out.printf("%s: NEW -> %s\n", getName(), order);
        exchange.submitOrder(order);

        myActiveOrders.add(order);
    }

    private void tryModifyOldOrder() {
        Order oldOrder = myActiveOrders.get(random.nextInt(myActiveOrders.size()));

        if (oldOrder.getQuantity() == 0) {
            myActiveOrders.remove(oldOrder);
            return;
        }

        long age = System.currentTimeMillis() - oldOrder.getCreatedAt();
        if (age < ORDER_TTL_MS) {
            return;
        }

        double newPrice = getModifiedPrice(oldOrder);

        System.out.printf("%s: REQUEST MODIFY (age %dms) -> %s to new price $%.2f\n",
                getName(), age, oldOrder, newPrice);

        exchange.modifyOrder(oldOrder.getOrderId(), newPrice);

        oldOrder.resetTimestamp();
    }

    private double getModifiedPrice(Order oldOrder) {
        double newPrice;
        if (oldOrder.getOrderType() == OrderType.BUY) {
            newPrice = oldOrder.getPrice() * 1.02;
        } else {
            newPrice = oldOrder.getPrice() * 0.98;
        }
        return Math.round(newPrice * 100.0) / 100.0;
    }
}
