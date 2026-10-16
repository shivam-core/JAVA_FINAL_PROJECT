package com.shivamcore.loanapp.util;

import java.io.Console;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Singleton wrapper around the application's single System.in Scanner.
 * Strictly prevents creating multiple Scanner instances.
 */
public class ConsoleInput {

    private final Scanner scanner;
    private final Console console;

    public ConsoleInput() {
        this(System.in);
    }

    public ConsoleInput(InputStream in) {
        this.scanner = new Scanner(in);
        this.console = System.console();
    }

    /**
     * Reads a single line of text from the console.
     */
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    /**
     * Reads a non-empty trimmed line from the console.
     */
    public String readRequiredLine(String prompt, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println(errorMessage);
        }
    }

    /**
     * Reads a password, masking input via System.console() when available,
     * falling back to standard scanner line reading in IDE environments.
     */
    public String readPassword(String prompt) {
        System.out.print(prompt);
        if (console != null) {
            char[] chars = console.readPassword();
            return chars != null ? new String(chars) : "";
        }
        return scanner.nextLine();
    }

    /**
     * Reads an integer menu selection or input.
     */
    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid integer.");
            }
        }
    }
}
