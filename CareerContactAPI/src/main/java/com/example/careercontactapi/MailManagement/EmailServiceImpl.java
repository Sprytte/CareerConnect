package com.example.careercontactapi.MailManagement;

import com.example.careercontactapi.JobApplicationManagement.datalayer.JobApplication;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.util.Properties;


@Slf4j
@Service
public class EmailServiceImpl implements EmailService{
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final JobPostingRepository jobPostingRepository;
    Session session;

    @Generated
    public EmailServiceImpl(@Value("${spring.mail.username}") String username,
                            @Value("${spring.mail.password}") String password,
                            JavaMailSender javaMailSender,
                            UserRepository userRepository,
                            JobPostingRepository jobPostingRepository) {

        this.mailSender = javaMailSender;
        this.userRepository = userRepository;
        this.jobPostingRepository = jobPostingRepository;

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        session = Session.getInstance(prop,
                new Authenticator() {
                    @Generated
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
    }

    @Override
    public String sendMail(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Email subject");
        message.setText("Email body");

        mailSender.send(message);
        return "Success!";
    }

    @Override
    public void sendUpdatedJobApplicationAlert(JobApplication jobApplication) {
        SimpleMailMessage message = new SimpleMailMessage();
        User user = userRepository.getReferenceById(jobApplication.getId());
        JobPosting jobPosting = jobPostingRepository.getReferenceById(jobApplication.getJobPostingId());

        message.setTo(user.getEmail());
        message.setSubject("Your application has been updated!");

        String body = "Hello " + user.getName() + "!" +
                    "\nThe status of your application for '" + jobPosting.getJobName() + "' has been updated " +
                    "and is now '" + jobApplication.getStatus() + "'." +
                    "\nTo view your application, " +
                    "<a href=\"http://localhost:3000/account/applications/" + jobApplication.getId() +
                    ">click here</a>";
        message.setText(body);

        mailSender.send(message);
    }

    //todo example on using an html template for the email content
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
