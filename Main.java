import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting simulation...");

        StockExchange exchange = new StockExchange();

        for (int i = 1; i <= 6; i++) {
            new BuyerSellerThread(exchange, "Trader-" + i).start();
        }

        Thread.sleep(15_000);

        System.out.println("\n\n--- SIMULATION ENDED ---");
        exchange.printMarketState();


        System.out.println("\n\n--- FINAL TRADE HISTORY ---");
        List<Trade> finalTrades = exchange.getTradeHistory();

        if (finalTrades.isEmpty()) {
            System.out.println("No trades were executed.");
        } else {
            for (Trade trade : finalTrades) {
                System.out.println(trade);
            }
            System.out.printf("\nTotal trades executed: %d\n", finalTrades.size());
        }

        System.exit(0);
    }
}