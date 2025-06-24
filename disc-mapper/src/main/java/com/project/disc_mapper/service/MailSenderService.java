package com.project.disc_mapper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class MailSenderService {

    @Autowired
    private JavaMailSender jms;


    public void sendEmail(String toEmail,
                          String toSubject,
                          String body) {

        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom("projauth0@gmail.com");
        mail.setTo(toEmail);
        mail.setText(body);
        mail.setSubject(toSubject);

        jms.send(mail);

        System.out.println("Mail Sent Successfully");
    }

    public String buildPasswordRecoveryEmail(String username,
                                             String code) {

        StringBuilder sb = new StringBuilder();

        sb.append("User ").append(username).append(",\n");
        sb.append("Your password recovery key is: ").append(code);

        return sb.toString();
    }

    public long getRemainingSeconds(LocalDateTime dbDateTime, int validitySeconds) {

        long elapsed = ChronoUnit.SECONDS.between(dbDateTime, LocalDateTime.now());
        return Math.max(0, validitySeconds - elapsed);
    }

    public boolean isValidToken(LocalDateTime dbDateTime, int validitySeconds) {
        long elapsed = ChronoUnit.SECONDS.between(dbDateTime, LocalDateTime.now());
        return elapsed <= validitySeconds;
    }
}
