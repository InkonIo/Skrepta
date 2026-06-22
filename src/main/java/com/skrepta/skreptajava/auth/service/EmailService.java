package com.skrepta.skreptajava.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@skrepta.kz");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendPasswordResetCode(String to, String resetCode) {
        String subject = "Сброс пароля для Skrepta";
        String text = String.format(
                "Здравствуйте!\n\n" +
                "Вы запросили сброс пароля для вашего аккаунта Skrepta. " +
                "Ваш 6-значный код для сброса пароля:\n\n" +
                "КОД: %s\n\n" +
                "Этот код действителен в течение 10 минут.\n\n" +
                "Если вы не запрашивали сброс пароля, просто проигнорируйте это письмо.\n\n" +
                "С уважением,\n" +
                "Команда Skrepta",
                resetCode
        );
        sendEmail(to, subject, text);
    }

    public void sendRegistrationConfirmationEmail(String to, String fio) {
        String subject = "Добро пожаловать в Skrepta!";
        String text = String.format(
                "Здравствуйте, %s!\n\n" +
                "Поздравляем с успешной регистрацией в приложении Skrepta.\n" +
                "Теперь вы можете войти в свой аккаунт, используя ваш email и пароль.\n\n" +
                "С уважением,\n" +
                "Команда Skrepta",
                fio
        );
        sendEmail(to, subject, text);
    }
}