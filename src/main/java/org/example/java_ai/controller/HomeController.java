package org.example.java_ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
            "service", "AI-Native智能商城API",
            "version", "1.0",
            "time", LocalDateTime.now().toString()
        );
    }
}
