package com.example.print.db;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Minimal Spring Boot context for Testcontainers-based integration tests. */
@SpringBootApplication(scanBasePackages = "com.example.print.db")
public class TestApplication {
}
