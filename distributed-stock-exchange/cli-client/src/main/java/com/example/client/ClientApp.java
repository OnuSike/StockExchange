package com.example.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.exchange.engine.OrderType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.UUID;

public class ClientApp {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Simple CLI client for the Exchange Server");

        while (true) {
            System.out.println("Commands: new, analyze, trades, exit");
            System.out.print("> ");
            String cmd = sc.nextLine().trim();
            if (cmd.equalsIgnoreCase("exit")) break;
            if (cmd.equalsIgnoreCase("new")) {
                System.out.print("Symbol: "); String sym = sc.nextLine().trim();
                System.out.print("BUY or SELL: "); OrderType type = OrderType.valueOf(sc.nextLine().trim().toUpperCase());
                System.out.print("Price: "); double price = Double.parseDouble(sc.nextLine().trim());
                System.out.print("Quantity: "); int qty = Integer.parseInt(sc.nextLine().trim());

                var body = new java.util.HashMap<String, Object>();
                body.put("stockSymbol", sym);
                body.put("orderType", type);
                body.put("price", price);
                body.put("quantity", qty);
                body.put("traderId", "cli-" + UUID.randomUUID().toString().substring(0,4));

                String json = mapper.writeValueAsString(body);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("Server returned orderId: " + resp.body());

            } else if (cmd.equalsIgnoreCase("analyze")) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/analyze?exchangeUrl=http://localhost:8080/api/trades"))
                        .GET().build();
                var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println(resp.body());

            } else if (cmd.equalsIgnoreCase("trades")) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/trades"))
                        .GET().build();
                var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println(resp.body());
            }
        }

        System.out.println("Client exiting.");
    }
}
