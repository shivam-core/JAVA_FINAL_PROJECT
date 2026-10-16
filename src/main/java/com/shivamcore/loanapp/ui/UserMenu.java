package com.shivamcore.loanapp.ui;

import com.shivamcore.loanapp.model.User;
import com.shivamcore.loanapp.util.ConsoleInput;

/**
 * Console menu presented to authenticated users.
 */
public class UserMenu {

    private final ConsoleInput input;

    public UserMenu(ConsoleInput input) {
        this.input = input;
    }

    /**
     * Runs the post-login user menu loop.
     *
     * @param user Currently authenticated user
     */
    public void show(User user) {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("Welcome, " + user.getFname() + " " + user.getLname());
        System.out.println("==========================================");

        boolean inSession = true;
        while (inSession) {
            System.out.println();
            System.out.println("--- User Menu ---");
            System.out.println("1. Logout");
            System.out.println("0. Logout");
            int choice = input.readInt("Enter choice: ");

            switch (choice) {
                case 1:
                case 0:
                    System.out.println("Logging out... You have been successfully logged out.");
                    inSession = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
}
