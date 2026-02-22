package com.shop.clothingstore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetPasswordEmail(String toEmail, String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Reset mật khẩu - Clothing Store");
        message.setText("""
                Bạn đã yêu cầu đặt lại mật khẩu.

                Click vào link sau để đặt lại mật khẩu:
                %s

                Link có hiệu lực trong 30 phút.
                """.formatted(resetLink));

        mailSender.send(message);
    }
}