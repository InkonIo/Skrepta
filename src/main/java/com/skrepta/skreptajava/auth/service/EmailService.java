package com.skrepta.skreptajava.auth.service;

import com.resend.Resend;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.password}")
    private String resendApiKey;

    private static final String LOGO_URL = "https://skrepta.kz/favicon.ico";

    private Resend resend;

    @PostConstruct
    public void init() {
        this.resend = new Resend(resendApiKey);
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                    .from("Skrepta <noreply@skrepta.kz>")
                    .to(to)
                    .subject(subject)
                    .html(text)
                    .build();

            SendEmailResponse response = resend.emails().send(sendEmailRequest);
            System.out.println("Email sent successfully. ID: " + response.getId());
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String wrapWithLayout(String bodyContent) {
        return String.format(
                "<html>" +
                "<body style='margin:0; padding:0; background-color:#f4f4f7; font-family: Arial, sans-serif;'>" +
                "  <table width='100%%' cellpadding='0' cellspacing='0' style='background-color:#f4f4f7; padding: 24px 0;'>" +
                "    <tr>" +
                "      <td align='center'>" +
                "        <table width='480' cellpadding='0' cellspacing='0' style='background-color:#ffffff; border-radius:8px; overflow:hidden;'>" +
                "          <tr>" +
                "            <td align='center' style='padding: 24px 0 8px 0;'>" +
                "              <img src='%s' alt='Skrepta' width='140' style='display:block;' />" +
                "            </td>" +
                "          </tr>" +
                "          <tr>" +
                "            <td style='padding: 16px 32px 32px 32px; color:#222222;'>" +
                "              %s" +
                "            </td>" +
                "          </tr>" +
                "        </table>" +
                "      </td>" +
                "    </tr>" +
                "  </table>" +
                "</body>" +
                "</html>",
                LOGO_URL,
                bodyContent
        );
    }

    public void sendPasswordResetCode(String to, String resetCode) {
        String subject = "Сброс пароля для Skrepta";
        String body =
                "<h3>Здравствуйте!</h3>" +
                "<p>Вы запросили сброс пароля для вашего аккаунта Skrepta. Ваш 6-значный код для сброса пароля:</p>" +
                "<h2 style='color: #2d89ef;'>КОД: " + resetCode + "</h2>" +
                "<p>Этот код действителен в течение 10 минут.</p>" +
                "<p>Если вы не запрашивали сброс пароля, просто проигнорируйте это письмо.</p>" +
                "<br>" +
                "<p>С уважением,<br>Команда Skrepta</p>";

        sendEmail(to, subject, wrapWithLayout(body));
    }

    public void sendRegistrationConfirmationEmail(String to, String fio) {
        String subject = "Добро пожаловать в Skrepta!";
        String body =
                "<h3>Здравствуйте, " + fio + "!</h3>" +
                "<p>Поздравляем с успешной регистрацией в приложении Skrepta.</p>" +
                "<p>Теперь вы можете войти в свой аккаунт, используя ваш email и пароль.</p>" +
                "<br>" +
                "<p>С уважением,<br>Команда Skrepta</p>";

        sendEmail(to, subject, wrapWithLayout(body));
    }
}