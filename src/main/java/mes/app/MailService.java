package mes.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {


    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String prenm, String uuid){
        String subject = "비밀번호 찾기 인증 메일입니다.";
        String text = "안녕하세요, " + prenm + "님.\n\n"
                + "다음 인증 코드를 입력하여 비밀번호 재설정을 완료하세요:\n"
                + uuid + "\n\n"
                + "이 코드는 3분 동안 유효합니다.";

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom("kimyouli0330@naver.com");

        mailSender.send(message);
    }

    public void saveVerificationEmail(String to, String prenm, String uuid){
        if (prenm == null || prenm.isEmpty()) {
            prenm = "사용자";
        }
        String subject = "사용자등록 인증 메일입니다.";
        String text = "안녕하세요, " + prenm + "님.\n\n"
                + "다음 인증 코드를 입력하여 사용자등록을 완료하세요:\n"
                + uuid + "\n\n"
                + "이 코드는 3분 동안 유효합니다.";

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom("kimyouli0330@naver.com");

        mailSender.send(message);
    }

}
