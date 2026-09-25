package com.example.careercontactapi.MailManagement;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;


@Slf4j
@Service
public class EmailServiceImpl implements EmailService{
//    private final JavaMailSender mailSender;
//    @Value("${spring.mail.username}")
//    private String sender;
//    Session session;

//    @Generated
//    public EmailServiceImpl(@Value("${spring.mail.username}") String username,
//                            @Value("${spring.mail.password}") String password,
//                            JavaMailSender javaMailSender) {
//
//        this.mailSender = javaMailSender;
//
//        Properties prop = new Properties();
//        prop.put("mail.smtp.host", "smtp.gmail.com");
//        prop.put("mail.smtp.port", "587");
//        prop.put("mail.smtp.auth", "true");
//        prop.put("mail.smtp.starttls.enable", "true");
//
//        session = Session.getInstance(prop,
//                new Authenticator() {
//                    @Generated
//                    protected PasswordAuthentication getPasswordAuthentication() {
//                        return new PasswordAuthentication(username, password);
//                    }
//                });
//    }
//
    @Override
    public String sendMail(String email) {
//        log.info("EMAIL: " + sender);
//
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(email);
//        message.setSubject("Email subject");
//        message.setText("Email body");
//
//        mailSender.send(message);
//
//        mailSender.send(message);
        return "Success!";
    }

    //todo example on  using an html template for the email content
//    @Override
//    public void sendOrderConfirmation(Transaction transaction) {
//        MimeMessage message = mailSender.createMimeMessage();
//
//        try {
//            message.setFrom(new InternetAddress(sender));
//            message.setRecipients(MimeMessage.RecipientType.TO, transaction.getEmail());
//            message.setSubject("Thank you for your order!");
//
//            InputStream resource = new ClassPathResource("/templates/OrderPlaced.html").getInputStream();
//            String htmlTemplate = IOUtils.toString(resource, "UTF-8");
//
//            htmlTemplate = htmlTemplate.replace("${orderId}", transaction.getTransactionId());
//            htmlTemplate = htmlTemplate.replace("${customerName}", transaction.getFirstName());
//            htmlTemplate = htmlTemplate.replace("${items}", getItems(transaction.getTransactionId()));
//
//            double sub = transaction.getAmount();
//            double shipping = transaction.getShippingCost();
//            double taxes = (transaction.getAmount() + transaction.getShippingCost()) * 0.15;
//            htmlTemplate = htmlTemplate.replace("${subtotal}", String.format("%,.2f$", sub));
//            htmlTemplate = htmlTemplate.replace("${shipping}", String.format("%,.2f$", shipping));
//            htmlTemplate = htmlTemplate.replace("${taxes}", String.format("%,.2f$", taxes));
//            htmlTemplate = htmlTemplate.replace("${total}", String.format("%,.2f$", sub + shipping + taxes));
//
//            message.setContent(htmlTemplate, "text/html; charset=utf-8");
//        } catch (Exception e){
//            throw new RuntimeException("Error sending email: " + e.getMessage());
//        }
//
//        mailSender.send(message);
//    }

}
