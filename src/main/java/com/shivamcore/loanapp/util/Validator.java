package com.shivamcore.loanapp.util;

import com.shivamcore.loanapp.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Validates user input according to assignment requirements.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("^[0-9]{10}$");

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern LETTERS_ONLY_PATTERN = Pattern.compile("^[A-Za-z]+$");

    private Validator() {
        // Utility class
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static void validateEmail(String email) {
        if (!isValidEmail(email)) {
            throw new ValidationException("Invalid email format. Please enter a valid email address.");
        }
    }

    public static boolean isValidMobile(String mobile) {
        return mobile != null && MOBILE_PATTERN.matcher(mobile.trim()).matches();
    }

    public static void validateMobile(String mobile) {
        if (!isValidMobile(mobile)) {
            throw new ValidationException("Mobile number must be exactly 10 digits.");
        }
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return UPPERCASE_PATTERN.matcher(password).find()
                && LOWERCASE_PATTERN.matcher(password).find()
                && DIGIT_PATTERN.matcher(password).find()
                && SPECIAL_CHAR_PATTERN.matcher(password).find();
    }

    public static void validatePassword(String password) {
        if (!isValidPassword(password)) {
            throw new ValidationException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && LETTERS_ONLY_PATTERN.matcher(name.trim()).matches();
    }

    public static void validateName(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is mandatory.");
        }
        if (!isValidName(value)) {
            throw new ValidationException(fieldName + " must contain only letters.");
        }
    }

    public static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && username.trim().length() >= 3 && username.trim().length() <= 50;
    }

    public static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username is mandatory.");
        }
        if (!isValidUsername(username)) {
            throw new ValidationException("Username must be between 3 and 50 characters.");
        }
    }

    public static void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new ValidationException("Location is mandatory.");
        }
        if (location.trim().length() > 100) {
            throw new ValidationException("Location cannot exceed 100 characters.");
        }
    }

    public static void validateUserRegistration(String fname, String lname, String mobile, String email,
                                                String location, String username, String password) {
        validateName("First Name (fname)", fname);
        validateName("Last Name (lname)", lname);
        validateMobile(mobile);
        validateEmail(email);
        validateLocation(location);
        validateUsername(username);
        validatePassword(password);
    }
}
