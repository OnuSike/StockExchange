package com.example.exchange.controller;
import com.example.exchange.engine.OrderBook;
import com.example.exchange.engine.Order;
import com.example.exchange.engine.StockExchange;
import com.example.exchange.engine.Trade;
import com.example.exchange.dto.ModifyRequest;
import com.example.exchange.dto.OrderRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExchangeController {

    private final StockExchange exchange = new StockExchange();

    @PostMapping("/orders")
    public ResponseEntity<String> submitOrder(@RequestBody OrderRequest req) {
        Order o = new Order(req.getStockSymbol(), req.getOrderType(), req.getPrice(), req.getQuantity(), req.getTraderId());
        exchange.submitOrder(o);
        return ResponseEntity.ok(o.getOrderId());
    }

    @PostMapping("/orders/{orderId}/modify")
    public ResponseEntity<Void> modifyOrder(@PathVariable String orderId, @RequestBody ModifyRequest req) {
        exchange.modifyOrder(orderId, req.getNewPrice());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        exchange.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    // Adaugă asta în ExchangeController.java
// Importă clasa OrderBook dacă e nevoie


    @GetMapping("/orderbook/data/{symbol}")
    public ResponseEntity<OrderBook> getOrderBookData(@PathVariable String symbol) {
        return ResponseEntity.ok(exchange.getOrderBook(symbol));
    }

    @GetMapping("/trades")
    public ResponseEntity<List<Trade>> trades() {
        return ResponseEntity.ok(exchange.getTradeHistory());
    }

    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<String> orderbook(@PathVariable String symbol) {
        // text dump
        exchange.printMarketState();
        return ResponseEntity.ok("Market state printed to server logs (demo)");
    }

    @GetMapping
    public String home() {
        return "Exchange server is running!";
    }
}