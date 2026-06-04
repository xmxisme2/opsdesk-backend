package com.opsdesk.auth.service.impl;

import com.opsdesk.auth.dto.CaptchaRequest;
import com.opsdesk.auth.service.CaptchaService;
import com.opsdesk.auth.vo.CaptchaVO;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码服务实现。
 *
 * <p>生成简单图片验证码并写入 Redis，登录时校验成功后立即删除，避免重复使用。</p>
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final int EXPIRES_SECONDS = 5 * 60;
    private static final String CANDIDATES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public CaptchaServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public CaptchaVO createCaptcha(CaptchaRequest request) {
        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + captchaId, code, Duration.ofSeconds(EXPIRES_SECONDS));
        return new CaptchaVO(captchaId, drawImage(code), EXPIRES_SECONDS);
    }

    @Override
    public void validate(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入图形验证码");
        }

        String key = CAPTCHA_KEY_PREFIX + captchaId;
        String expectedCode = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(expectedCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图形验证码已过期，请刷新后重试");
        }

        stringRedisTemplate.delete(key);
        if (!expectedCode.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图形验证码错误");
        }
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            builder.append(CANDIDATES.charAt(secureRandom.nextInt(CANDIDATES.length())));
        }
        return builder.toString();
    }

    private String drawImage(String code) {
        try {
            int width = 120;
            int height = 42;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(244, 247, 251));
            graphics.fillRect(0, 0, width, height);

            for (int i = 0; i < 12; i++) {
                graphics.setColor(randomMutedColor());
                int x1 = secureRandom.nextInt(width);
                int y1 = secureRandom.nextInt(height);
                int x2 = secureRandom.nextInt(width);
                int y2 = secureRandom.nextInt(height);
                graphics.drawLine(x1, y1, x2, y2);
            }

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(new Color(31, 64, 115));
                graphics.drawString(String.valueOf(code.charAt(i)), 18 + i * 24, 29 + secureRandom.nextInt(4));
            }
            graphics.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图形验证码生成失败");
        }
    }

    private Color randomMutedColor() {
        return new Color(140 + secureRandom.nextInt(80), 150 + secureRandom.nextInt(70), 160 + secureRandom.nextInt(60));
    }
}
