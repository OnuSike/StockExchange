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

    private final BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private final Thread engineThread;

    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Map<String, Order> activeOrders = new ConcurrentHashMap<>();
    private final List<Trade> tradeHistory = new ArrayList<>();

    public StockExchange() {
        String[] stocks = {"AAPL", "MSFT", "GOOGL", "INTC", "AMD", "NVDA"};
        for (String stock : stocks) {
            orderBooks.put(stock, new OrderBook());
        }
        this.engineThread = new Thread(this::runEngine, "StockEngineThread");
        this.engineThread.start();
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

            // Update quantities
            newOrder.setQuantity(newOrder.getQuantity() - tradeQuantity);
            bestOppositeOrder.setQuantity(bestOppositeOrder.getQuantity() - tradeQuantity);

            if (bestOppositeOrder.getQuantity() > 0) {
                oppositeBook.add(bestOppositeOrder);
            } else {
                activeOrders.remove(bestOppositeOrder.getOrderId());
            }

            if (newOrder.getQuantity() == 0) {
                activeOrders.remove(newOrder.getOrderId());
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
            System.out.println("Engine cancelled: " + orderToCancel);
        }
    }

    private void processModifyOrder(String orderId, double newPrice) {
        Order orderToModify = activeOrders.get(orderId);
        if (orderToModify == null) return;

        OrderBook book = orderBooks.get(orderToModify.getStockSymbol());
        if (book == null) return;

        book.removeOrder(orderToModify);

        System.out.printf("Engine: MODIFYING order to new price $%.2f\n", newPrice);
        orderToModify.setPrice(newPrice);

        book.addOrder(orderToModify);
    }
}