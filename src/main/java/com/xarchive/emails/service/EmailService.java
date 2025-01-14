package com.xarchive.emails.service;

import com.xarchive.config.ApplicationProperties;
import com.xarchive.emails.pojo.EmailTemplateLoader;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;
    @Autowired
    private ApplicationProperties appProperties;


    public boolean sendPinEmail(String to, String subject, String pin) {
        String filePath = "/templates/email-template.html"; // class path for jar
        String htmlTemplate = EmailTemplateLoader.loadEmailTemplate(filePath);

        if (htmlTemplate == null) {
            return false;
        }
        String emailContent = htmlTemplate.replace("${PIN}", pin);

        return sendEmail(to, subject, emailContent);
    }

    private boolean sendEmail(String to, String subject, String emailContent) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.getMailUsername());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(emailContent, true);

            emailSender.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }

    }
}
