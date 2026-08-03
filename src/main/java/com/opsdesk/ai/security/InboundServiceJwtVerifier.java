package com.opsdesk.ai.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * 主应用内部接口 Service JWT 校验器。
 *
 * <p>只接受 AI 服务签发的短期 HS256 令牌，普通用户 JWT 无法访问内部知识快照。</p>
 */
@Component
public class InboundServiceJwtVerifier {
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String SERVICE_NAME = "opsdesk-ai-service";
    private final ObjectMapper objectMapper;
    private final AiServiceProperties properties;

    public InboundServiceJwtVerifier(ObjectMapper objectMapper, AiServiceProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void verify(String token) {
        if (!StringUtils.hasText(properties.getServiceJwtSecret())
                || properties.getServiceJwtSecret().length() < MIN_SECRET_LENGTH) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, "AI Service JWT 密钥未配置或长度不足");
        }
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少 Service JWT");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 格式错误");
        }
        Map<String, Object> header = decode(parts[0]);
        if (!"HS256".equals(String.valueOf(header.get("alg")))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 算法不受支持");
        }
        String expected = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 签名无效");
        }
        validate(decode(parts[1]));
    }

    private void validate(Map<String, Object> claims) {
        long now = Instant.now().getEpochSecond();
        long skew = Math.max(0L, properties.getMaxClockSkewSeconds());
        long issuedAt = number(claims.get("iat"));
        long expiresAt = number(claims.get("exp"));
        if (!properties.getInboundIssuer().equals(String.valueOf(claims.get("iss")))
                || !properties.getInboundAudience().equals(String.valueOf(claims.get("aud")))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 签发方或受众无效");
        }
        if (!"SERVICE".equals(String.valueOf(claims.get("tokenType")))
                || !SERVICE_NAME.equals(String.valueOf(claims.get("service")))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "服务身份无权访问主应用内部接口");
        }
        if (issuedAt > now + skew || expiresAt <= now - skew || expiresAt - issuedAt > 300L) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 已过期或有效期非法");
        }
        if (!StringUtils.hasText(claims.get("jti") == null ? null : String.valueOf(claims.get("jti")))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 缺少唯一标识");
        }
    }

    private Map<String, Object> decode(String value) {
        try {
            return objectMapper.readValue(Base64.getUrlDecoder().decode(value), new TypeReference<>() { });
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 载荷无效");
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getServiceJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Service JWT 校验失败");
        }
    }

    private long number(Object value) {
        try {
            return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Service JWT 时间字段无效");
        }
    }
}
