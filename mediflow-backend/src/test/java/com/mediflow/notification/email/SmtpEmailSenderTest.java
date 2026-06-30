package com.mediflow.notification.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

@Mock
private JavaMailSender javaMailSender;

@Test
void sendsEmailWithExpectedContent() {
    SmtpEmailSender emailSender =
        new SmtpEmailSender(
            javaMailSender,
            "noreply@mediflow.example"
        );

    emailSender.send(
        "patient@example.com",
        "Appointment confirmed",
        "Your appointment has been confirmed."
    );

    ArgumentCaptor<SimpleMailMessage> captor =
        ArgumentCaptor.forClass(
            SimpleMailMessage.class
        );

    verify(javaMailSender).send(
        captor.capture()
    );

    SimpleMailMessage message =
        captor.getValue();

    assertThat(message.getFrom())
        .isEqualTo(
            "noreply@mediflow.example"
        );

    assertThat(message.getTo())
        .containsExactly(
            "patient@example.com"
        );

    assertThat(message.getSubject())
        .isEqualTo(
            "Appointment confirmed"
        );

    assertThat(message.getText())
        .isEqualTo(
            "Your appointment has been confirmed."
        );
}

}