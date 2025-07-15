package mes.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {
    @Value("${spring.mail.password}")
    private String mailPassword;

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost("smtp.naver.com");
        mailSender.setPort(465);
        mailSender.setUsername("kimyouli0330@naver.com");
        mailSender.setPassword(mailPassword); // 반드시 앱 비밀번호

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.starttls.enable", "false");
        // props.put("mail.debug", "true");

        return mailSender;
    }
    /*@Bean
    public JavaMailSender getJavaMailSender(){   //TODO: 네이버 내꺼 쓰고있는데 이거 바꾸자.
        JavaMailSenderImpl mailSender  = new JavaMailSenderImpl();

        mailSender.setHost("smtp.naver.com");
        mailSender.setUsername("replusshare@naver.com");
        mailSender.setPassword("actas@5020");

        mailSender.setPort(587);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.ssl.trust", "smtp.naver.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return mailSender;
    }*/


}
