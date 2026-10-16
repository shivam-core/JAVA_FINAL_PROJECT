package com.shivamcore.loanapp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    @DisplayName("Environment variable takes precedence over file property")
    void envTakesPrecedenceOverFile() {
        Properties fileProps = new Properties();
        fileProps.setProperty("db.url", "jdbc:mysql://localhost:3306/file_db");

        Map<String, String> env = new HashMap<>();
        env.put("DB_URL", "jdbc:mysql://localhost:3306/env_db");

        ConfigLoader loader = new ConfigLoader(fileProps, env);
        assertEquals("jdbc:mysql://localhost:3306/env_db", loader.get("db.url"));
    }

    @Test
    @DisplayName("Falls back to properties file when env var is absent")
    void fallbackToPropertyFile() {
        Properties fileProps = new Properties();
        fileProps.setProperty("smtp.port", "587");

        ConfigLoader loader = new ConfigLoader(fileProps, new HashMap<>());
        assertEquals("587", loader.get("smtp.port"));
        assertEquals(587, loader.getIntOrDefault("smtp.port", 25));
    }

    @Test
    @DisplayName("Throws IllegalStateException when required key is missing")
    void throwsWhenMissing() {
        ConfigLoader loader = new ConfigLoader(new Properties(), new HashMap<>());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> loader.get("missing.key"));
        assertTrue(ex.getMessage().contains("Missing required configuration key: missing.key"));
    }

    @Test
    @DisplayName("Returns default value when key is absent")
    void returnsDefaultValue() {
        ConfigLoader loader = new ConfigLoader(new Properties(), new HashMap<>());
        assertEquals("defaultVal", loader.getOrDefault("some.key", "defaultVal"));
        assertEquals(42, loader.getIntOrDefault("some.port", 42));
        assertTrue(loader.getBooleanOrDefault("some.bool", true));
    }
}
