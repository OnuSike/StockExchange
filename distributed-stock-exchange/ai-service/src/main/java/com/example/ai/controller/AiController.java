package com.example.ai.controller;

import com.example.ai.service.AiAnalyzer;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AiController {

    private final AiAnalyzer analyzer = new AiAnalyzer();

    @GetMapping("/analyze")
    public String analyze(@RequestParam(value = "exchangeUrl", defaultValue = "http://localhost:8080/api/trades") String exchangeUrl) {
        return analyzer.analyzeFromExchange(exchangeUrl);
    }
}
