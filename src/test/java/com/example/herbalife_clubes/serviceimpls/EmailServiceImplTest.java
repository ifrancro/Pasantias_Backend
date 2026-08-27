package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "appName", "Test App");
    }

    @Test
    void messagingExceptionSeConvierteEnEmailDeliveryException() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(inv -> {
            throw new MessagingException("Authentication failed smtp-relay.brevo.com secret=XYZ");
        });

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> emailService.sendVerificationCode("u@test.com", "Ana", "123456"));

        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, ex.getMessage());
        assertInstanceOf(MessagingException.class, ex.getCause());
    }

    @Test
    void mailSendExceptionSeConvierteEnEmailDeliveryException() throws Exception {
        MimeMessage message = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP relay rejected"))
                .when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryException ex = assertThrows(
                EmailDeliveryException.class,
                () -> emailService.sendVerificationCode("u@test.com", "Ana", "123456"));

        assertEquals(EmailDeliveryException.DEFAULT_MESSAGE, ex.getMessage());
        assertInstanceOf(MailSendException.class, ex.getCause());
    }
}
