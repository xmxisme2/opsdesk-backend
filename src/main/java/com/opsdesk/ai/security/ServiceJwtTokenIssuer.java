package com.opsdesk.ai.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 主应用 Service JWT 签发器。
 *
 * <p>令牌只用于短期调用独立 AI 服务，不能替代用户 access token。</p>
 */
@Component
public class ServiceJwtTokenIssuer {

    /** Service JWT 类型：AI 服务据此拒绝普通用户令牌。 */
    private static final String TOKEN_TYPE_SERVICE = "SERVICE";
    /** 当前调用方服务名：必须与 AI 服务允许列表一致。 */
    private static final String SERVICE_NAME = "opsdesk-backend";
    /** HMAC 共享密钥最低长度。 */
    private static final int MIN_SECRET_LENGTH = 32;
    /** 服务令牌最大有效期，防止配置错误签发长期凭据。 */
    private static final long MAX_TOKEN_TTL_SECONDS = 300L;

    private final ObjectMapper objectMapper;
    private final AiServiceProperties properties;

    public ServiceJwtTokenIssuer(ObjectMapper objectMapper, AiServiceProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String issue(CurrentUser currentUser) {
        ensureSecretConfigured();
        long now = Instant.now().getEpochSecond();
        long ttl = Math.min(Math.max(1L, properties.getTokenTtlSeconds()), MAX_TOKEN_TTL_SECONDS);

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jti", UUID.randomUUID().toString().replace("-", ""));
        payload.put("iss", properties.getIssuer());
        payload.put("aud", properties.getAudience());
        payload.put("sub", SERVICE_NAME);
        payload.put("service", SERVICE_NAME);
        payload.put("tokenType", TOKEN_TYPE_SERVICE);
        payload.put("iat", now);
        payload.put("exp", now + ttl);
        if (currentUser != null) {
            payload.put("userId", String.valueOf(currentUser.getUserId()));
            payload.put("roles", currentUser.getRoles());
        }

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String content = encodedHeader + "." + encodedPayload;
        return content + "." + sign(content);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getServiceJwtSecret())
                && properties.getServiceJwtSecret().length() >= MIN_SECRET_LENGTH;
    }

    private void ensureSecretConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    "AI Service JWT 密钥未配置或长度不足"
            );
        }
    }

    private String encodeJson(Object value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Service JWT 载荷序列化失败");
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getServiceJwtSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Service JWT 签名失败");
        }
    }
}
