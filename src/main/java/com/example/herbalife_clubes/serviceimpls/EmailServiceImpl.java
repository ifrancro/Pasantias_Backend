package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import com.example.herbalife_clubes.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String LOGO_CID = "expandeLogo";
    private static final String LOGO_CLASSPATH = "templates/email/expande-logo.png";

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${app.name:EXPANDE}")
    private String appName;

    @Override
    public void sendVerificationCode(String to, String name, String code) {
        sendOtpEmail(to, name, code, appName + " - Código de Verificación",
                "Gracias por registrarte. Usa el siguiente código<br>para verificar tu correo electrónico:",
                "Si no solicitaste esta verificación, puedes ignorar este correo de forma segura.",
                "código de verificación");
    }

    @Override
    public void sendPasswordResetCode(String to, String name, String code) {
        sendOtpEmail(to, name, code, appName + " - Restablecer contraseña",
                "Recibimos una solicitud para restablecer tu contraseña.<br>Usa el siguiente código:",
                "Si no solicitaste este cambio, ignora este correo. Tu contraseña actual seguirá siendo válida.",
                "código de recuperación");
    }

    private void sendOtpEmail(
            String to,
            String name,
            String code,
            String subject,
            String bodyIntro,
            String footerNote,
            String logLabel) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildHtmlContent(name, code, bodyIntro, footerNote), true);
            helper.addInline(LOGO_CID, new ClassPathResource(LOGO_CLASSPATH));

            mailSender.send(message);
            log.info("{} enviado a: {}", logLabel, to);

        } catch (MessagingException | MailException e) {
            log.error("Error al enviar {} a {}", logLabel, to, e);
            throw new EmailDeliveryException(e);
        }
    }

    /**
     * Construye el contenido HTML del correo con el diseño de marca EXPANDE.
     */
    private String buildHtmlContent(
            String name,
            String code,
            String bodyIntro,
            String footerNote) {
        // Separar el código en dígitos individuales, espaciados, sin cajas
        StringBuilder codeDigits = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            if (i > 0) {
                codeDigits.append("<td style=\"width:10px;\"></td>");
            }
            codeDigits.append(String.format(
                "<td style=\"font-family:Arial,sans-serif;font-size:34px;font-weight:400;" +
                "color:#0E1B31;letter-spacing:2px;\">%c</td>", code.charAt(i)));
        }

        return "<!DOCTYPE html>" +
            "<html lang=\"es\">" +
            "<head><meta charset=\"UTF-8\"></head>" +
            "<body style=\"margin:0;padding:0;background-color:#F2F4F7;font-family:Arial,Helvetica,sans-serif;\">" +
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#F2F4F7;padding:40px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table width=\"520\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border:1px solid #E8EBF0;" +
            "border-radius:16px;overflow:hidden;\">" +
            // Header con logo
            "<tr><td style=\"padding:32px 40px 24px;text-align:center;border-bottom:1px solid #EDF0F4;\">" +
            "<img src=\"cid:" + LOGO_CID + "\" alt=\"" + appName + "\" height=\"34\" style=\"height:34px;width:auto;border:0;\">" +
            "</td></tr>" +
            // Cuerpo del mensaje
            "<tr><td style=\"padding:40px;text-align:center;\">" +
            "<p style=\"color:#0E1B31;font-size:20px;font-weight:700;margin:0 0 12px;\">Hola " + name + ",</p>" +
            "<p style=\"color:#5A6B84;font-size:15px;line-height:1.6;margin:0 0 28px;\">" +
            bodyIntro + "</p>" +
            // Código OTP centrado, sin cajas
            "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto 24px;\"><tr>" + codeDigits + "</tr></table>" +
            // Divisor con reloj
            "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 18px;\"><tr>" +
            "<td style=\"border-top:1px solid #E4E8EE;\"></td>" +
            "<td width=\"24\" style=\"text-align:center;color:#A9B4C4;font-size:14px;\">&#8987;</td>" +
            "<td style=\"border-top:1px solid #E4E8EE;\"></td>" +
            "</tr></table>" +
            "<p style=\"color:#3C4C66;font-size:15px;margin:0 0 28px;\">Este código expira en <strong>15 minutos</strong>.</p>" +
            "<p style=\"color:#9AA6B8;font-size:13px;line-height:1.5;margin:0;\">" +
            footerNote + "</p>" +
            "</td></tr>" +
            // Footer
            "<tr><td style=\"background:#F7F9FB;padding:16px 40px;text-align:center;border-top:1px solid #EDF0F4;\">" +
            "<p style=\"color:#9AA6B8;font-size:11px;margin:0;\">© 2026 " + appName + ". Todos los derechos reservados.</p>" +
            "</td></tr>" +
            "</table></td></tr></table></body></html>";
    }
}
