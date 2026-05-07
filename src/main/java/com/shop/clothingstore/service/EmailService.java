package com.shop.clothingstore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetPasswordEmail(String toEmail, String resetLink) {
        String from = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "no-reply@clothingstore.com";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Password Reset - Clothing Store");
        message.setText("""
                You have requested to reset your password.

                Click the following link to reset your password:
                %s

                This link will expire in 30 minutes.
                """.formatted(resetLink));

        mailSender.send(message);
    }

    public void sendRegistrationEmail(String toEmail) {
        String from = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "no-reply@clothingstore.com";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Welcome to Clothing Store!");
        message.setText("""
                Hello!

                Thank you for registering at Clothing Store.
                Your account has been successfully created.

                We hope you enjoy shopping with us!

                Best regards,
                Clothing Store Team
                """);

        mailSender.send(message);
    }
}