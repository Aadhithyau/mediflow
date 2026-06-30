package com.mediflow.notification.email;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@EnabledIfEnvironmentVariable(
named = "MAIL_USERNAME",
matches = ".+@.+"
)
@EnabledIfEnvironmentVariable(
named = "MAIL_PASSWORD",
matches = ".+"
)
@EnabledIfEnvironmentVariable(
named = "MAIL_TEST_RECIPIENT",
matches = ".+@.+"
)
class SmtpEmailSenderLiveTest {

@Test
void sendsRealEmailThroughConfiguredSmtpServer() {
    String username =
        System.getenv("MAIL_USERNAME");

    String password =
        System.getenv("MAIL_PASSWORD");

    String recipient =
        System.getenv("MAIL_TEST_RECIPIENT");

    JavaMailSenderImpl mailSender =
        new JavaMailSenderImpl();

    mailSender.setHost("smtp.gmail.com");
    mailSender.setPort(587);
    mailSender.setUsername(username);
    mailSender.setPassword(password);

    Properties properties =
        mailSender.getJavaMailProperties();

    properties.put(
        "mail.smtp.auth",
        "true"
    );

    properties.put(
        "mail.smtp.starttls.enable",
        "true"
    );

    properties.put(
        "mail.smtp.starttls.required",
        "true"
    );

    SimpleMailMessage message =
        new SimpleMailMessage();

    message.setFrom(username);
    message.setTo(recipient);
    message.setSubject(
        "MediFlow email configuration verified"
    );
    message.setText("""
        Hello,

        This is a live email sent by the MediFlow backend.

        The Gmail SMTP configuration is working successfully, and MediFlow is ready to send appointment confirmations, cancellations, and reminders.

        Regards,
        MediFlow
        """);

    mailSender.send(message);
}

}