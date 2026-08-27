package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.exceptions.EmailDeliveryException;
import com.example.herbalife_clubes.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${app.name:Nutrilife Club}")
    private String appName;

    @Override
    public void sendVerificationCode(String to, String name, String code) {
        sendOtpEmail(to, name, code, appName + " - Código de Verificación",
                "Verificación de Correo Electrónico",
                "Gracias por registrarte. Usa el siguiente código para verificar tu correo electrónico:",
                "Si no solicitaste esta verificación, puedes ignorar este correo de forma segura.",
                "código de verificación");
    }

    @Override
    public void sendPasswordResetCode(String to, String name, String code) {
        sendOtpEmail(to, name, code, appName + " - Restablecer contraseña",
                "Recuperación de contraseña",
                "Recibimos una solicitud para restablecer tu contraseña. Usa el siguiente código:",
                "Si no solicitaste este cambio, ignora este correo. Tu contraseña actual seguirá siendo válida.",
                "código de recuperación");
    }

    private void sendOtpEmail(
            String to,
            String name,
            String code,
            String subject,
            String headerSubtitle,
            String bodyIntro,
            String footerNote,
            String logLabel) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildHtmlContent(name, code, headerSubtitle, bodyIntro, footerNote), true);

            mailSender.send(message);
            log.info("{} enviado a: {}", logLabel, to);

        } catch (MessagingException | MailException e) {
            log.error("Error al enviar {} a {}", logLabel, to, e);
            throw new EmailDeliveryException(e);
        }
    }

    /**
     * Construye el contenido HTML del correo con diseño profesional.
     */
    private String buildHtmlContent(
            String name,
            String code,
            String headerSubtitle,
            String bodyIntro,
            String footerNote) {
        // Separar el código en caracteres individuales para el diseño
        StringBuilder codeBoxes = new StringBuilder();
        for (char c : code.toCharArray()) {
            codeBoxes.append(String.format(
                "<span style=\"display:inline-block;width:44px;height:52px;line-height:52px;" +
                "text-align:center;font-size:28px;font-weight:bold;color:#1B5E20;" +
                "background:#E8F5E9;border:2px solid #4CAF50;border-radius:10px;" +
                "margin:0 4px;font-family:'Inter',Arial,sans-serif;\">%c</span>", c));
        }

        return "<!DOCTYPE html>" +
            "<html lang=\"es\">" +
            "<head><meta charset=\"UTF-8\"></head>" +
            "<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:'Inter',Arial,sans-serif;\">" +
            "<table width=\"100%%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f4f4f4;padding:40px 0;\">" +
            "<tr><td align=\"center\">" +
            "<table width=\"500\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#ffffff;border-radius:16px;" +
            "box-shadow:0 4px 20px rgba(0,0,0,0.08);overflow:hidden;\">" +
            // Header con gradiente verde
            "<tr><td style=\"background:linear-gradient(135deg,#4CAF50,#1B5E20);padding:32px 40px;text-align:center;\">" +
            "<h1 style=\"color:#ffffff;margin:0;font-size:24px;font-weight:700;\">🌿 " + appName + "</h1>" +
            "<p style=\"color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;\">" + headerSubtitle + "</p>" +
            "</td></tr>" +
            // Cuerpo del mensaje
            "<tr><td style=\"padding:40px;\">" +
            "<p style=\"color:#333;font-size:16px;margin:0 0 8px;\">Hola <strong>" + name + "</strong>,</p>" +
            "<p style=\"color:#666;font-size:14px;line-height:1.6;margin:0 0 28px;\">" +
            bodyIntro + "</p>" +
            // Código OTP centrado
            "<div style=\"text-align:center;margin:0 0 28px;\">" + codeBoxes + "</div>" +
            // Alerta de expiración
            "<div style=\"background:#FFF8E1;border-left:4px solid #FF9800;padding:12px 16px;border-radius:0 8px 8px 0;margin:0 0 28px;\">" +
            "<p style=\"color:#E65100;font-size:13px;margin:0;\">⏱ Este código expira en <strong>15 minutos</strong>.</p>" +
            "</div>" +
            "<p style=\"color:#999;font-size:12px;line-height:1.5;margin:0;\">" +
            footerNote + "</p>" +
            "</td></tr>" +
            // Footer
            "<tr><td style=\"background:#F5F5F5;padding:20px 40px;text-align:center;border-top:1px solid #eee;\">" +
            "<p style=\"color:#aaa;font-size:11px;margin:0;\">© 2026 " + appName + ". Todos los derechos reservados.</p>" +
            "</td></tr>" +
            "</table></td></tr></table></body></html>";
    }
}
