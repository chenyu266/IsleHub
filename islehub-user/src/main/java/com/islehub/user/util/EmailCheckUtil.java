package com.islehub.user.util;

import com.islehub.common.enums.EmailErrorEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.security.SecureRandom;

/**
 * 邮箱验证码工具类，提供验证码生成、邮件发送及发送错误信息匹配功能
 */

@Component
@RequiredArgsConstructor
public class EmailCheckUtil {
    @Value("${spring.mail.username}")
    private String hostEmail;

    private final JavaMailSender mailSender;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int BOUND = 1_000_000;

    public String generateCode() {
        int randomNum;
        do {
            randomNum = SECURE_RANDOM.nextInt(BOUND);
        } while (randomNum == 0);
        return String.format("%06d", randomNum);
    }

    public String errorMatcher(String errorMsg) {
        if (errorMsg == null) return "邮件发送失败，请稍后重试";

        String lowerMsg = errorMsg.toLowerCase();

        for (EmailErrorEnum e : EmailErrorEnum.values()) {
            if (lowerMsg.contains(e.getKeyword())) {
                return e.getMessage();
            }
        }

        return "邮件发送失败，请检查邮箱或稍后再试";
    }

    public void sendCode(String toEmail, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(hostEmail);
        helper.setTo(toEmail);
        helper.setSubject("邮箱验证验证码");
        helper.setText("你的验证码为：<b>" + code + "</b>，2分钟内有效", true);
        mailSender.send(message);
    }
}