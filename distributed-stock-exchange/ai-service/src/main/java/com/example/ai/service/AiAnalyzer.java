package com.example.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AiAnalyzer {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String analyzeFromExchange(String exchangeTradesUrl) {
        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(exchangeTradesUrl))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            List<Map<String, Object>> trades = mapper.readValue(resp.body(), new TypeReference<>() {});

            return performMachineLearningAnalysis(trades);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "AI Service Error: " + e.getMessage();
        }
    }

    private String performMachineLearningAnalysis(List<Map<String, Object>> trades) {
        if (trades == null || trades.isEmpty()) return "Nu există date suficiente pentru antrenare.";

        StringBuilder sb = new StringBuilder();
        sb.append("🤖 AI PREDICTION (Linear Regression Model):\n");


        Map<String, List<Map<String, Object>>> tradesBySymbol = trades.stream()
                .collect(Collectors.groupingBy(t -> (String) t.get("stockSymbol")));

        tradesBySymbol.forEach((symbol, symbolTrades) -> {

            if (symbolTrades.size() < 2) {
                sb.append(String.format("• %s: Date insuficiente (%d tranzacție)\n", symbol, symbolTrades.size()));
                return;
            }



            List<Double> prices = symbolTrades.stream()
                    .map(t -> ((Number) t.get("price")).doubleValue())
                    .collect(Collectors.toList());


            SimpleRegression model = new SimpleRegression();
            for (int i = 0; i < prices.size(); i++) {
                model.addData(i, prices.get(i));
            }

            double currentPrice = prices.get(prices.size() - 1);
            double slope = model.getSlope();


            double nextPricePrediction = model.predict(prices.size());


            double trendStrength = Math.abs(slope) * 100;

            String emoji = slope > 0 ? "📈" : (slope < 0 ? "📉" : "➡️");
            String advice = "";

            if (nextPricePrediction > currentPrice * 1.01) advice = "STRONG BUY (Creștere așteptată)";
            else if (nextPricePrediction < currentPrice * 0.99) advice = "PANIC SELL (Scădere așteptată)";
            else advice = "HOLD (Piață stabilă)";

            sb.append(String.format("• %s %s\n", symbol, emoji));
            sb.append(String.format("  - Preț actual: $%.2f\n", currentPrice));
            sb.append(String.format("  - Predicție AI: $%.2f (Panta: %.4f)\n", nextPricePrediction, slope));
            sb.append(String.format("  - Sfat: %s\n\n", advice));
        });

        return sb.toString();
    }



    static class SimpleRegression {
        private double sumX = 0;
        private double sumY = 0;
        private double sumXY = 0;
        private double sumX2 = 0;
        private long n = 0;

        public void addData(double x, double y) {
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
            n++;
        }

        public double getSlope() {
            if (n < 2) return 0;
            return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        }

        public double getIntercept() {
            return (sumY - getSlope() * sumX) / n;
        }

        public double predict(double x) {
            return getSlope() * x + getIntercept();
        }
    }
}