package com.shivamcore.loanapp.config;

import com.shivamcore.loanapp.exception.DataAccessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @Test
    @DisplayName("Throws DataAccessException on unreachable database connection")
    void throwsOnUnreachableDatabase() {
        DatabaseManager dbManager = new DatabaseManager(
                "jdbc:mysql://localhost:19999/non_existent_db?connectTimeout=1000",
                "invalid_user",
                "invalid_pass"
        );
        assertThrows(DataAccessException.class, dbManager::getConnection);
        assertFalse(dbManager.testConnection());
    }

    @Test
    @DisplayName("Initializes cleanly with valid URL and parameters")
    void initializesCleanly() {
        DatabaseManager dbManager = new DatabaseManager(
                "jdbc:mysql://localhost:3306/loan_app",
                "root",
                "secret"
        );
        assertEquals("jdbc:mysql://localhost:3306/loan_app", dbManager.getUrl());
    }
}
