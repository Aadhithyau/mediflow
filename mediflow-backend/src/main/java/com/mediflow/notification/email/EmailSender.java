package com.mediflow.notification.email;

public interface EmailSender {

void send(
    String recipient,
    String subject,
    String body
);

}