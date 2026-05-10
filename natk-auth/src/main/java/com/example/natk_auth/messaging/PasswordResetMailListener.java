package com.example.natk_auth.messaging;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetMailListener {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @RabbitListener(queues = "auth.password-reset.requested")
    public void handle(PasswordResetRequestedEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(from);
            helper.setTo(event.email());
            helper.setSubject("Password reset");
            helper.setText("""
                    <html>
                      <body>
                        <p>You requested a password reset.</p>
                        <p><a href="%s">Reset password</a></p>
                        <p>This link is valid for 30 minutes.</p>
                      </body>
                    </html>
                    """.formatted(event.resetLink()), true);

            mailSender.send(message);
            log.info("Password reset email sent to {}", event.email());
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to build reset email", e);
        }
    }
}
