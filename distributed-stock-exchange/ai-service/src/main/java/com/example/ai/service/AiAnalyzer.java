package com.example.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

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

            return localAnalysis(trades);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "AI service failed to get trades: " + e.getMessage();
        }
    }

    private String localAnalysis(List<Map<String, Object>> trades) {
        if (trades == null || trades.isEmpty()) return "No trades to analyze.";

        Map<String, Double> avg = trades.stream().collect(java.util.stream.Collectors.groupingBy(
                t -> (String) t.get("stockSymbol"),
                java.util.stream.Collectors.averagingDouble(t -> ((Number) t.get("price")).doubleValue())
        ));

        StringBuilder sb = new StringBuilder();
        sb.append("AI ANALYSIS (local fallback):\n");
        avg.forEach((sym, mean) -> sb.append(String.format("%s -> avg price: $%.2f\n", sym, mean)));

        Map<String, Double> lastPrice = trades.stream().collect(java.util.stream.Collectors.toMap(
                t -> (String) t.get("stockSymbol"),
                t -> ((Number) t.get("price")).doubleValue(),
                (a, b) -> b // keep last
        ));

        lastPrice.forEach((sym, last) -> {
            double m = avg.get(sym);
            String trend = last > m ? "upward" : (last < m ? "downward" : "flat");
            sb.append(String.format("%s trend: %s (last=%.2f, avg=%.2f)\n", sym, trend, last, m));
        });

        sb.append("\n(You can enable an external LLM by providing an API client in AiAnalyzer.java)");
        return sb.toString();
    }
}
