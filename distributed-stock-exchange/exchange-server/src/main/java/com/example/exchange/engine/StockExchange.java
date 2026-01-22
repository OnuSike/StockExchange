package com.example.exchange.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class StockExchange {

    interface Event {}
    static class NewOrderEvent implements Event {
        final Order order;
        NewOrderEvent(Order order) { this.order = order; }
    }
    static class CancelOrderEvent implements Event {
        final String orderId;
        CancelOrderEvent(String orderId) { this.orderId = orderId; }
    }
    static class ModifyOrderEvent implements Event {
        final String orderId;
        final double newPrice;
        ModifyOrderEvent(String orderId, double newPrice) {
            this.orderId = orderId;
            this.newPrice = newPrice;
        }
    }
    static class ClaimAlertEvent implements Event {
        final String alertId;
        final String orderId;
        final String buyerId;
        ClaimAlertEvent(String alertId, String orderId, String buyerId) {
            this.alertId = alertId;
            this.orderId = orderId;
            this.buyerId = buyerId;
        }
    }

    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final Thread engineThread;

    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<String, Order> activeOrders = new ConcurrentHashMap<>();
    private final List<Trade> tradeHistory = new ArrayList<>();
    private final BlockingQueue<Alert> alertQueue = new LinkedBlockingQueue<>();
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final Map<String, String> alertByOrderId = new ConcurrentHashMap<>();

    private static final double ALERT_PRICE_THRESHOLD = 30.0;

    public StockExchange() {
        String[] stocks = {"AAPL", "MSFT", "GOOGL", "INTC", "AMD", "NVDA"};
        for (String stock : stocks) {
            orderBooks.put(stock, new OrderBook());
        }
        this.engineThread = new Thread(this::runEngine, "StockEngineThread");
        this.engineThread.start();
    }


    public OrderBook getOrderBook(String stockSymbol) {
        return orderBooks.get(stockSymbol);
    }

    public void submitOrder(Order order) {
        try {
            eventQueue.put(new NewOrderEvent(order));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void cancelOrder(String orderId) {
        try {
            eventQueue.put(new CancelOrderEvent(orderId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void modifyOrder(String orderId, double newPrice) {
        try {
            eventQueue.put(new ModifyOrderEvent(orderId, newPrice));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public List<Trade> getTradeHistory() {
        synchronized (tradeHistory) {
            return new ArrayList<>(this.tradeHistory);
        }
    }

    public List<Alert> getActiveAlerts(String traderId) {
        drainAlertQueue();
        List<Alert> alerts = new ArrayList<>();
        for (Alert alert : activeAlerts.values()) {
            if (!alert.isClaimed() && !alert.getSellerId().equals(traderId)) {
                alerts.add(alert);
            }
        }
        return alerts;
    }

    public boolean claimAlert(String alertId, String buyerId) {
        Alert alert = activeAlerts.get(alertId);
        if (alert == null || alert.isClaimed()) return false;
        if (alert.getSellerId().equals(buyerId)) return false;

        synchronized (alert) {
            Order sellOrder = activeOrders.get(alert.getOrderId());
            if (sellOrder == null || sellOrder.getQuantity() <= 0) {
                return false;
            }
            if (!alert.claim()) return false;
            try {
                eventQueue.put(new ClaimAlertEvent(alertId, alert.getOrderId(), buyerId));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    public void printMarketState() {
        System.out.println("\n--- MARKET STATE ---");
        for (Map.Entry<String, OrderBook> entry : orderBooks.entrySet()) {
            System.out.printf("--- %s ---\n", entry.getKey());
            Order bestAsk = entry.getValue().getAsks().peek();
            Order bestBid = entry.getValue().getBids().peek();
            String askStr = (bestAsk != null) ? String.format("$%.2f (%d)", bestAsk.getPrice(), entry.getValue().getAsks().size()) : "---";
            String bidStr = (bestBid != null) ? String.format("$%.2f (%d)", bestBid.getPrice(), entry.getValue().getBids().size()) : "---";
            System.out.printf("ASKS: %-15s | BIDS: %-15s\n", askStr, bidStr);
        }
        System.out.println("--------------------");
    }

    private void runEngine() {
        System.out.println("Stock Engine is running.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Event event = eventQueue.take();

                if (event instanceof NewOrderEvent) {
                    processNewOrder(((NewOrderEvent) event).order);
                } else if (event instanceof CancelOrderEvent) {
                    processCancelOrder(((CancelOrderEvent) event).orderId);
                } else if (event instanceof ModifyOrderEvent) {
                    ModifyOrderEvent modEvent = (ModifyOrderEvent) event;
                    processModifyOrder(modEvent.orderId, modEvent.newPrice);
                } else if (event instanceof ClaimAlertEvent) {
                    ClaimAlertEvent claimEvent = (ClaimAlertEvent) event;
                    processClaimAlert(claimEvent.alertId, claimEvent.orderId, claimEvent.buyerId);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Stock Engine was interrupted.");
        }
    }

    private void processNewOrder(Order newOrder) {
        OrderBook book = orderBooks.get(newOrder.getStockSymbol());
        if (book == null) return;

        activeOrders.put(newOrder.getOrderId(), newOrder);

        if (newOrder.getOrderType() == OrderType.BUY) {
            match(newOrder, book.getAsks(), book);
        } else {
            match(newOrder, book.getBids(), book);
        }

        if (newOrder.getQuantity() > 0) {
            book.addOrder(newOrder);
            maybeCreateLowPriceAlert(newOrder);
        }
    }

    private void match(Order newOrder, PriorityQueue<Order> oppositeBook, OrderBook book) {

        while (newOrder.getQuantity() > 0 && !oppositeBook.isEmpty()) {

            Order bestOppositeOrder = null;
            List<Order> skippedOrders = new ArrayList<>();

            while (!oppositeBook.isEmpty()) {
                Order topOrder = oppositeBook.poll();

                if (topOrder.getTraderId().equals(newOrder.getTraderId())) {
                    skippedOrders.add(topOrder);
                    continue;
                }

                boolean isPriceMatch;
                if (newOrder.getOrderType() == OrderType.BUY) {
                    isPriceMatch = newOrder.getPrice() >= topOrder.getPrice();
                } else {
                    isPriceMatch = newOrder.getPrice() <= topOrder.getPrice();
                }

                if (isPriceMatch) {
                    bestOppositeOrder = topOrder;
                    break;
                } else {
                    skippedOrders.add(topOrder);
                    break;
                }
            }

            oppositeBook.addAll(skippedOrders);

            if (bestOppositeOrder == null) {
                break;
            }

            System.out.println("Engine: MATCH FOUND! (Not a self-trade)");

            int tradeQuantity = Math.min(newOrder.getQuantity(), bestOppositeOrder.getQuantity());
            double tradePrice = bestOppositeOrder.getPrice();

            String buyerId, sellerId;
            if (newOrder.getOrderType() == OrderType.BUY) {
                buyerId = newOrder.getTraderId();
                sellerId = bestOppositeOrder.getTraderId();
            } else {
                buyerId = bestOppositeOrder.getTraderId();
                sellerId = newOrder.getTraderId();
            }
            Trade trade = new Trade(newOrder.getStockSymbol(), tradeQuantity, tradePrice, buyerId, sellerId);

            synchronized (tradeHistory) {
                tradeHistory.add(trade);
            }
            System.out.println(trade);


            newOrder.setQuantity(newOrder.getQuantity() - tradeQuantity);
            bestOppositeOrder.setQuantity(bestOppositeOrder.getQuantity() - tradeQuantity);

            if (bestOppositeOrder.getQuantity() > 0) {
                oppositeBook.add(bestOppositeOrder);
                updateAlertQuantity(bestOppositeOrder.getOrderId(), bestOppositeOrder.getQuantity());
            } else {
                activeOrders.remove(bestOppositeOrder.getOrderId());
                removeAlertForOrder(bestOppositeOrder.getOrderId());
            }

            if (newOrder.getQuantity() == 0) {
                activeOrders.remove(newOrder.getOrderId());
                removeAlertForOrder(newOrder.getOrderId());
                break;
            }
        }
    }

    private void processCancelOrder(String orderId) {
        Order orderToCancel = activeOrders.get(orderId);
        if (orderToCancel == null) return;

        OrderBook book = orderBooks.get(orderToCancel.getStockSymbol());
        if (book != null) {
            book.removeOrder(orderToCancel);
            activeOrders.remove(orderId);
            removeAlertForOrder(orderId);
            System.out.println("Engine cancelled: " + orderToCancel);
        }
    }

    private void processModifyOrder(String orderId, double newPrice) {
        Order orderToModify = activeOrders.get(orderId);
        if (orderToModify == null) return;

        OrderBook book = orderBooks.get(orderToModify.getStockSymbol());
        if (book == null) return;


        book.removeOrder(orderToModify);
        removeAlertForOrder(orderId);

        System.out.printf("Engine: MODIFYING order to new price $%.2f\n", newPrice);
        orderToModify.setPrice(newPrice);
        orderToModify.resetTimestamp();



        if (orderToModify.getOrderType() == OrderType.BUY) {
            match(orderToModify, book.getAsks(), book);
        } else {
            match(orderToModify, book.getBids(), book);
        }


        if (orderToModify.getQuantity() > 0) {
            book.addOrder(orderToModify);
            maybeCreateLowPriceAlert(orderToModify);
        } else {
            activeOrders.remove(orderId);
        }
    }

    private void processClaimAlert(String alertId, String orderId, String buyerId) {
        Order sellOrder = activeOrders.get(orderId);
        if (sellOrder == null || sellOrder.getQuantity() <= 0) {
            removeAlertForOrder(orderId);
            return;
        }

        OrderBook book = orderBooks.get(sellOrder.getStockSymbol());
        if (book == null) {
            removeAlertForOrder(orderId);
            return;
        }

        book.removeOrder(sellOrder);
        removeAlertForOrder(orderId);

        int tradeQuantity = sellOrder.getQuantity();
        double tradePrice = sellOrder.getPrice();
        Trade trade = new Trade(sellOrder.getStockSymbol(), tradeQuantity, tradePrice, buyerId, sellOrder.getTraderId());
        synchronized (tradeHistory) {
            tradeHistory.add(trade);
        }
        System.out.println(trade);

        sellOrder.setQuantity(0);
        activeOrders.remove(sellOrder.getOrderId());
    }

    private void maybeCreateLowPriceAlert(Order order) {
        if (order.getOrderType() != OrderType.SELL) return;
        if (order.getPrice() >= ALERT_PRICE_THRESHOLD) return;
        if (order.getQuantity() <= 0) return;
        if (alertByOrderId.containsKey(order.getOrderId())) return;

        Alert alert = new Alert(order.getOrderId(), order.getStockSymbol(), order.getPrice(), order.getQuantity(), order.getTraderId());
        activeAlerts.put(alert.getId(), alert);
        alertByOrderId.put(order.getOrderId(), alert.getId());
        alertQueue.offer(alert);
    }

    private void removeAlertForOrder(String orderId) {
        String alertId = alertByOrderId.remove(orderId);
        if (alertId != null) {
            activeAlerts.remove(alertId);
        }
    }

    private void updateAlertQuantity(String orderId, int quantity) {
        String alertId = alertByOrderId.get(orderId);
        if (alertId == null) return;
        Alert alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.setQuantity(quantity);
        }
    }

    private void drainAlertQueue() {
        List<Alert> drained = new ArrayList<>();
        alertQueue.drainTo(drained);
    }
}
