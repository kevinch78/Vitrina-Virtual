package com.vitrina.vitrinaVirtual.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Restablecimiento de Contraseña - Vitrina Virtual");
        
        // Ahora el enlace apunta a una página de tu frontend, que luego llamará a nuestra API.
        // Por ejemplo: http://localhost:3000/reset-password?token=EL_TOKEN
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        
        message.setText("Hola,\n\n"
                + "Has solicitado restablecer tu contraseña.\n\n"
                + "Usa el siguiente token en tu aplicación o haz clic en el enlace para establecer una nueva contraseña:\n"
                + "Token: " + token + "\n" // Aseguramos que el token sea visible
                + "Haz clic en el siguiente enlace para establecer una nueva contraseña:\n\n"
                + resetUrl + "\n\n"
                + "Este token expirará en 15 minutos.\n\n"
                + "Si no solicitaste esto, por favor ignora este correo.");
        mailSender.send(message);
    }
}