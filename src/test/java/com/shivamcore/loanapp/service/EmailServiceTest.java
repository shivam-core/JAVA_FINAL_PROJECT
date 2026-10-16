package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    @Test
    void testMockEmailDispatch() {
        Properties props = new Properties();
        props.setProperty("mail.enabled", "false");
        ConfigLoader config = new ConfigLoader(props, null);
        EmailService emailService = new EmailService(config);

        assertTrue(emailService.sendActivationEmail("user@example.com", "Test"));
        assertTrue(emailService.sendDeactivationEmail("user@example.com", "Test"));
        assertTrue(emailService.sendDeletionEmail("user@example.com", "Test"));
        assertTrue(emailService.sendEditEmail("user@example.com", "Test"));
    }
}
