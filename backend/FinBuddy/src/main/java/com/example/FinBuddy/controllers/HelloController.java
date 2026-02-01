package com.example.FinBuddy.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple health check controller
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HelloController {

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "FinBuddy API is running!");
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Welcome to FinBuddy - Your Financial Portfolio Manager!";
    }
}
