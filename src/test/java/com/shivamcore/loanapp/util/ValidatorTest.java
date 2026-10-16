package com.shivamcore.loanapp.util;

import com.shivamcore.loanapp.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailTests {
        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "first.last@domain.co.in",
                "user+alias@sub.domain.org",
                "shivam.kore123@outlook.com"
        })
        void validEmails(String email) {
            assertTrue(Validator.isValidEmail(email));
            assertDoesNotThrow(() -> Validator.validateEmail(email));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "plainaddress",
                "@missingusername.com",
                "missingatsign.com",
                "two@@at.com",
                "user@.com",
                "user@domain.c",
                "user@domain",
                "",
                "   "
        })
        void invalidEmails(String email) {
            assertFalse(Validator.isValidEmail(email));
            assertThrows(ValidationException.class, () -> Validator.validateEmail(email));
        }

        @Test
        void nullEmail() {
            assertFalse(Validator.isValidEmail(null));
            assertThrows(ValidationException.class, () -> Validator.validateEmail(null));
        }
    }

    @Nested
    @DisplayName("Mobile Validation Tests")
    class MobileTests {
        @ParameterizedTest
        @ValueSource(strings = {
                "9876543210",
                "1234567890",
                "0000000000",
                "9999999999"
        })
        void validMobile(String mobile) {
            assertTrue(Validator.isValidMobile(mobile));
            assertDoesNotThrow(() -> Validator.validateMobile(mobile));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "123456789",     // 9 digits
                "12345678901",   // 11 digits
                "98765abcde",    // alphanumeric
                "98765-4321",    // with hyphen
                "",
                "          "
        })
        void invalidMobile(String mobile) {
            assertFalse(Validator.isValidMobile(mobile));
            assertThrows(ValidationException.class, () -> Validator.validateMobile(mobile));
        }

        @Test
        void nullMobile() {
            assertFalse(Validator.isValidMobile(null));
            assertThrows(ValidationException.class, () -> Validator.validateMobile(null));
        }
    }

    @Nested
    @DisplayName("Password Policy Tests")
    class PasswordTests {
        @ParameterizedTest
        @ValueSource(strings = {
                "Admin@123",
                "Secure#Password1",
                "P@ssw0rd2026",
                "!Aa1bbccdd"
        })
        void validPasswords(String password) {
            assertTrue(Validator.isValidPassword(password));
            assertDoesNotThrow(() -> Validator.validatePassword(password));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Short1!",          // 7 characters (min is 8)
                "alllowercase1!",   // missing uppercase
                "ALLUPPERCASE1!",   // missing lowercase
                "NoDigitsHere!",    // missing digit
                "NoSpecialChar123", // missing special char
                "",
                "        "
        })
        void invalidPasswords(String password) {
            assertFalse(Validator.isValidPassword(password));
            assertThrows(ValidationException.class, () -> Validator.validatePassword(password));
        }

        @Test
        void nullPassword() {
            assertFalse(Validator.isValidPassword(null));
            assertThrows(ValidationException.class, () -> Validator.validatePassword(null));
        }
    }

    @Nested
    @DisplayName("Name Validation Tests")
    class NameTests {
        @ParameterizedTest
        @ValueSource(strings = {"Amruta", "Patil", "Shivam", "Kore", "A", "JohnDoe"})
        void validNames(String name) {
            assertTrue(Validator.isValidName(name));
            assertDoesNotThrow(() -> Validator.validateName("First Name", name));
        }

        @ParameterizedTest
        @ValueSource(strings = {"John123", "Mary-Jane", "User!", "1234", "", "   "})
        void invalidNames(String name) {
            assertFalse(Validator.isValidName(name));
            assertThrows(ValidationException.class, () -> Validator.validateName("Name", name));
        }
    }

    @Nested
    @DisplayName("User Registration Aggregate Validation Tests")
    class AggregateRegistrationTests {
        @Test
        void validRegistrationSucceeds() {
            assertDoesNotThrow(() -> Validator.validateUserRegistration(
                    "Shivam",
                    "Kore",
                    "9876543210",
                    "shivam@example.com",
                    "Pune",
                    "shivamkore",
                    "Secure@123"
            ));
        }

        @Test
        void invalidEmailFailsRegistration() {
            ValidationException ex = assertThrows(ValidationException.class, () ->
                    Validator.validateUserRegistration(
                            "Shivam",
                            "Kore",
                            "9876543210",
                            "bad-email",
                            "Pune",
                            "shivamkore",
                            "Secure@123"
                    ));
            assertTrue(ex.getMessage().contains("Invalid email format"));
        }
    }
}
