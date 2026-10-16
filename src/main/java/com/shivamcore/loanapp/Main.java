package com.shivamcore.loanapp;

import com.shivamcore.loanapp.config.ConfigLoader;
import com.shivamcore.loanapp.config.DatabaseManager;
import com.shivamcore.loanapp.dao.JdbcUserDao;
import com.shivamcore.loanapp.dao.UserDao;
import com.shivamcore.loanapp.service.AuthService;
import com.shivamcore.loanapp.service.UserService;
import com.shivamcore.loanapp.ui.MainMenu;
import com.shivamcore.loanapp.util.ConsoleInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Loan Application System.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Loan Application System...");

        try {
            ConfigLoader configLoader = new ConfigLoader();
            DatabaseManager databaseManager = new DatabaseManager(configLoader);

            UserDao userDao = new JdbcUserDao(databaseManager);
            UserService userService = new UserService(userDao);
            AuthService authService = new AuthService(userDao);

            ConsoleInput consoleInput = new ConsoleInput();
            MainMenu mainMenu = new MainMenu(userService, authService, consoleInput);

            mainMenu.run();
        } catch (Exception e) {
            logger.error("Fatal startup error: {}", e.getMessage(), e);
            System.err.println("Fatal Application Error: " + e.getMessage());
        }
    }
}
