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
}