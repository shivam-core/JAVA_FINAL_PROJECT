package com.shivamcore.loanapp.service;

import com.shivamcore.loanapp.config.ConfigLoader;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Service for sending email notifications using Jakarta Mail.
 * Supports real SMTP delivery and a mock logging mode (mail.enabled=false).
 */
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final ConfigLoader config;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final Session mailSession;

    public EmailService(ConfigLoader config) {
        this.config = config;
        this.mailEnabled = config.getBooleanOrDefault("mail.enabled", false);
        this.fromAddress = config.getOrDefault("mail.from", "noreply@loanapp.com");

        if (mailEnabled) {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getOrDefault("smtp.host", "smtp.gmail.com"));
            props.put("mail.smtp.port", config.getOrDefault("smtp.port", "587"));
            props.put("mail.smtp.auth", config.getOrDefault("smtp.auth", "true"));
            props.put("mail.smtp.starttls.enable", config.getOrDefault("smtp.starttls.enable", "true"));

            String username = config.getOrDefault("smtp.username", "");
            String password = config.getOrDefault("smtp.password", "");

            this.mailSession = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            logger.info("EmailService initialized with active SMTP transport.");
        } else {
            this.mailSession = null;
            logger.info("EmailService initialized in MOCK mode (mail.enabled=false). Emails will be logged to file.");
        }
    }

    public boolean sendEmail(String toEmail, String subject, String body) {
        if (!mailEnabled) {
            logger.info("[MOCK EMAIL DELIVERED] To: {} | Subject: '{}' | Content: {}", toEmail, subject, body);
            return true;
        }

        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            logger.info("Email successfully dispatched via SMTP to {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to deliver email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    // Stage 2 exact notification templates
    public boolean sendActivationEmail(String toEmail, String fname) {
        String subject = "Account Update Notification";
        String body = "Dear " + fname + ", your account has been successfully activated by the admin. You can now log in to the system.";
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendDeactivationEmail(String toEmail, String fname) {
        String subject = "Account Update Notification";
        String body = "Dear " + fname + ", your account has been deactivated. Please contact support for more information.";
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendDeletionEmail(String toEmail, String fname) {
        String subject = "Account Update Notification";
        String body = "Dear " + fname + ", your account has been deleted from the system. If this was a mistake, please contact support immediately.";
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendEditEmail(String toEmail, String fname) {
        String subject = "Account Update Notification";
        String body = "Dear " + fname + ", your account details have been updated. If you did not request this change, please contact support immediately.";
        return sendEmail(toEmail, subject, body);
    }

    // Stage 3 loan notification templates
    public boolean sendLoanApprovedEmail(String toEmail, String fname, java.math.BigDecimal loanAmount) {
        String subject = "Your Loan Application has been Approved!";
        String body = "Dear " + fname + ", your loan application for " + loanAmount + " has been approved. Please check your dashboard for further details.";
        return sendEmail(toEmail, subject, body);
    }

    // Stage 5 loan approval notification with EMI breakdown
    public boolean sendLoanApprovedEmail(String toEmail, String fname, java.math.BigDecimal loanAmount,
                                         com.shivamcore.loanapp.model.LoanType loanType, int tenureMonths,
                                         java.math.BigDecimal interestRate, java.math.BigDecimal emi,
                                         java.math.BigDecimal totalInterest, java.math.BigDecimal totalPayable) {
        String subject = "Your Loan Application has been Approved!";
        String body = "Dear " + fname + ",\n\n" +
                "Congratulations! Your loan application has been approved.\n\n" +
                "Loan Sanction Breakdown:\n" +
                "- Loan Type: " + (loanType != null ? loanType.getDisplayName() : "Loan") + "\n" +
                "- Sanctioned Amount: ₹" + loanAmount + "\n" +
                "- Loan Duration: " + tenureMonths + " months\n" +
                "- Applied Interest Rate: " + interestRate + "%\n" +
                "- Monthly EMI: ₹" + emi + "\n" +
                "- Total Interest: ₹" + totalInterest + "\n" +
                "- Total Payable: ₹" + totalPayable + "\n\n" +
                "Thank you for choosing our services.";
        return sendEmail(toEmail, subject, body);
    }

    public boolean sendLoanRejectedEmail(String toEmail, String fname, String reason) {
        String subject = "Your Loan Application has been Rejected";
        String body = "Dear " + fname + ", we regret to inform you that your loan application has been rejected. Reason: " + reason + ". Please contact support for more details.";
        return sendEmail(toEmail, subject, body);
    }

    // Stage 4 guest credential notification template
    public boolean sendCredentialsEmail(String toEmail, String fname, String username, String password) {
        String subject = "Your Username and Password for Loan Application is";
        String body = "Dear " + fname + ",\n\n" +
                "Your account has been successfully created for your loan application.\n" +
                "Username: " + username + "\n" +
                "Password: " + password + "\n\n" +
                "Please keep your credentials safe.";
        return sendEmail(toEmail, subject, body);
    }
}
