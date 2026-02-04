package com.example.FinBuddy.services;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import javax.mail.MessagingException;  // Correct import

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email with the PDF report attached.
     *
     * @param to      The recipient's email address.
     * @param subject The subject of the email.
     * @param text    The body of the email.
     * @param pdfBytes The PDF report as a byte array.
     */
    public void sendEmailWithAttachment(String to, String subject, String text, byte[] pdfBytes) {
        try {
            // Create MimeMessage from mailSender
            MimeMessage message = mailSender.createMimeMessage();

            // MimeMessageHelper is used to build the email with attachments
            MimeMessageHelper helper = new MimeMessageHelper(message, true); // true = multipart (for attachments)

            // Set email details
            helper.setTo(to);               // Set recipient email address
            helper.setSubject(subject);     // Set subject of the email
            helper.setText(text);           // Set the body text of the email

            // Attach the PDF report
            helper.addAttachment("portfolio_report.pdf", new ByteArrayResource(pdfBytes));

            // Send the email using mailSender
            mailSender.send(message);

        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
