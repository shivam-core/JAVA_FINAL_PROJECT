package com.shivamcore.loanapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Loads configuration with precedence:
 * Environment Variable -> application.properties (filesystem or classpath) -> fail-fast.
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private final Properties fileProperties;
    private final Map<String, String> environmentVariables;

    public ConfigLoader() {
        this(loadPropertiesFile(), System.getenv());
    }

    public ConfigLoader(Properties fileProperties, Map<String, String> environmentVariables) {
        this.fileProperties = fileProperties != null ? fileProperties : new Properties();
        this.environmentVariables = environmentVariables != null ? environmentVariables : Collections.emptyMap();
    }

    private static Properties loadPropertiesFile() {
        Properties props = new Properties();

        // 1. Try file in working directory / application root
        File localFile = new File("application.properties");
        if (localFile.exists() && localFile.isFile()) {
            try (InputStream in = new FileInputStream(localFile)) {
                props.load(in);
                logger.info("Loaded configuration from working directory application.properties");
                return props;
            } catch (Exception e) {
                logger.warn("Failed to load local application.properties: {}", e.getMessage());
            }
        }

        // 2. Try classpath
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
                logger.info("Loaded configuration from classpath application.properties");
                return props;
            }
        } catch (Exception e) {
            logger.warn("Failed to load classpath application.properties: {}", e.getMessage());
        }

        return props;
    }

    /**
     * Resolves key by checking environment variable first (converted to UPPER_SNAKE_CASE),
     * then checking properties file.
     */
    public String get(String key) {
        String val = resolve(key);
        if (val == null || val.trim().isEmpty()) {
            throw new IllegalStateException("Missing required configuration key: " + key + 
                    " (check environment variable " + toEnvVarName(key) + " or application.properties)");
        }
        return val.trim();
    }

    public String getOrDefault(String key, String defaultValue) {
        String val = resolve(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        return val.trim();
    }

    public int getIntOrDefault(String key, int defaultValue) {
        String val = resolve(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer configuration for key '{}': '{}'. Using default: {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBooleanOrDefault(String key, boolean defaultValue) {
        String val = resolve(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    private String resolve(String key) {
        String envKey = toEnvVarName(key);
        if (environmentVariables.containsKey(envKey)) {
            return environmentVariables.get(envKey);
        }
        if (environmentVariables.containsKey(key)) {
            return environmentVariables.get(key);
        }
        return fileProperties.getProperty(key);
    }

    private String toEnvVarName(String key) {
        return key.toUpperCase().replace('.', '_').replace('-', '_');
    }
}
