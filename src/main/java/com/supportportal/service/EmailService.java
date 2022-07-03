package com.supportportal.service;

import com.sun.mail.smtp.SMTPTransport;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

import static com.supportportal.constant.EmailConstant.*;

@Service
public class EmailService {

    public void sendNewPasswordEmail(String firstName, String password, String email)
    {
        Message message =  createEmail(firstName,password,email);
        try {
            SMTPTransport smtpTransport = (SMTPTransport) getMailSession().getTransport(SIMPLE_MAIL_TRANSFER_PROTOCOL);
            smtpTransport.connect(GMAIL_SMTP_SERVER,USERNAME, PASSWORD);
            smtpTransport.sendMessage(message,message.getAllRecipients());
            smtpTransport.close();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private Message createEmail(String firstName, String password, String email) {

        Message message = new MimeMessage(getMailSession());
        try {
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email, false));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(CC_EMAIL, false));
            message.setSubject(EMAIL_SUBJECT);
            message.setText("Hello " + firstName + ", \n \n Your new account password is: " +password + "\n \n The support team");
            message.setSentDate(new Date());
            message.saveChanges();
            return message;
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Session getMailSession()
    {
        Properties properties = System.getProperties();
        properties.put(SMTP_HOST, GMAIL_SMTP_SERVER);
        properties.put(SMTP_AUTH, true);
        properties.put(SMTP_PORT, DEFAULT_PORT);
        properties.put(SMTP_STARTTLS_ENABLE, true);
        properties.put(SMTP_STARTTLS_REQUIRED, true);
        return Session.getInstance(properties,null);
    }
}
