package com.vk.rag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class DatabaseHealthController {

    private final DataSource dataSource;

    public DatabaseHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health/db")
    public String databaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return "Database connected: " + connection.getMetaData().getDatabaseProductName();
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }

    @GetMapping("/health/vector")
    public String vectorHealth() {

        try (Connection connection = dataSource.getConnection()) {

            var statement = connection.createStatement();

            var result = statement.executeQuery("""
            SELECT extversion
            FROM pg_extension
            WHERE extname = 'vector'
        """);

            if (result.next()) {
                return "pgvector enabled: " + result.getString("extversion");
            }

            return "pgvector is NOT enabled";

        } catch (SQLException e) {
            return "Vector check failed: " + e.getMessage();
        }
    }
}
