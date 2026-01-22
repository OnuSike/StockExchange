package com.example.ai.controller;

import com.example.ai.service.AiAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AiController {

    private final AiAnalyzer analyzer = new AiAnalyzer();
    private final String defaultExchangeUrl;

    public AiController(@Value("${AI_EXCHANGE_URL:${ai.exchange.url:http://localhost:8080/api/trades}}") String defaultExchangeUrl) {
        this.defaultExchangeUrl = defaultExchangeUrl;
    }

    @GetMapping("/analyze")
    public String analyze(@RequestParam(value = "exchangeUrl", required = false) String exchangeUrl) {
        String urlToUse = (exchangeUrl == null || exchangeUrl.isBlank()) ? defaultExchangeUrl : exchangeUrl;
        return analyzer.analyzeFromExchange(urlToUse);
    }
}
