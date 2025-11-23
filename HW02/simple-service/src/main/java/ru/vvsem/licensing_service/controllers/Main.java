package ru.vvsem.licensing_service.controllers;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Main {

    @RequestMapping("/")
    @ResponseStatus(code = HttpStatus.OK)
    public Map<String, String> mainPage() {
        return Map.of(
                "service", "simple-service",
                "status", "running",
                "healthCheck", "http://localhost:8000/health");
    }

    @RequestMapping("/health")
    @ResponseStatus(code = HttpStatus.OK)
    public Map<String, String> healthCheck() {
        return Collections.singletonMap("status", "OK");
    }
}
