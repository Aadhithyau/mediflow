package com.mediflow.notification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {

private final JavaMailSender javaMailSender;
private final String senderAddress;

public SmtpEmailSender(
    JavaMailSender javaMailSender,
    @Value("${app.mail.from}") String senderAddress
) {
    this.javaMailSender = javaMailSender;
    this.senderAddress = senderAddress;
}

@Override
public void send(
    String recipient,
    String subject,
    String body
) {
    SimpleMailMessage message =
        new SimpleMailMessage();

    message.setFrom(senderAddress);
    message.setTo(recipient);
    message.setSubject(subject);
    message.setText(body);

    javaMailSender.send(message);
}

}