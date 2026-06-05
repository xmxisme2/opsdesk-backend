package com.opsdesk.auth.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.auth.model.TokenClaims;
import com.opsdesk.auth.model.TokenPair;
import com.opsdesk.auth.service.TokenService;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JWT 登录令牌服务实现。
 *
 * <p>使用 HMAC-SHA256 生成 JWT，并通过 Redis 保存 refresh token 会话和 access token 黑名单。</p>
 */
@Service
public class TokenServiceImpl implements TokenService {

    /** access token 类型标识：仅用于短期访问令牌，禁止作为刷新令牌使用。 */
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";

    /** refresh token 类型标识：仅用于刷新会话，不能直接访问业务接口。 */
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    /** access token 黑名单 Redis Key 前缀：退出登录后按 tokenId 写入剩余有效期。 */
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    /** refresh token 会话 Redis Key 前缀：按 tokenId 保存所属用户 ID。 */
    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    /** 用户会话集合 Redis Key 前缀：用于踢出其他设备或失效全部 refresh token。 */
    private static final String USER_SESSION_KEY_PREFIX = "auth:user:sessions:";

    /** access token 默认有效期：首版固定为 2 小时。 */
    private static final long ACCESS_EXPIRES_SECONDS = 2 * 60 * 60L;

    /** refresh token 默认有效期：未勾选记住登录时固定为 7 天。 */
    private static final long REFRESH_EXPIRES_SECONDS = 7 * 24 * 60 * 60L;

    /** 记住登录 refresh token 有效期：勾选记住登录时固定为 30 天。 */
    private static final long REMEMBER_REFRESH_EXPIRES_SECONDS = 30 * 24 * 60 * 60L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final byte[] secretBytes;

    public TokenServiceImpl(StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper,
                            @Value("${opsdesk.security.jwt-secret}") String jwtSecret) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public TokenPair issueTokenPair(Long userId, boolean rememberMe) {
        long now = Instant.now().getEpochSecond();
        long refreshExpiresIn = rememberMe ? REMEMBER_REFRESH_EXPIRES_SECONDS : REFRESH_EXPIRES_SECONDS;

        String accessToken = createToken(userId, TOKEN_TYPE_ACCESS, now, now + ACCESS_EXPIRES_SECONDS, rememberMe);
        String refreshToken = createToken(userId, TOKEN_TYPE_REFRESH, now, now + refreshExpiresIn, rememberMe);
        TokenClaims refreshClaims = parseRefreshToken(refreshToken);
        saveRefreshSession(userId, refreshClaims.tokenId(), refreshExpiresIn);

        return new TokenPair(accessToken, ACCESS_EXPIRES_SECONDS, refreshToken, refreshExpiresIn);
    }

    @Override
    public TokenClaims parseAccessToken(String token) {
        TokenClaims claims = parseAndValidate(token, TOKEN_TYPE_ACCESS);
        String blacklistValue = stringRedisTemplate.opsForValue().get(BLACKLIST_KEY_PREFIX + claims.tokenId());
        if (StringUtils.hasText(blacklistValue)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        return claims;
    }

    @Override
    public TokenClaims parseRefreshToken(String token) {
        return parseAndValidate(token, TOKEN_TYPE_REFRESH);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        TokenClaims claims = parseRefreshToken(refreshToken);
        String refreshKey = REFRESH_KEY_PREFIX + claims.tokenId();
        String sessionUserId = stringRedisTemplate.opsForValue().get(refreshKey);
        if (!String.valueOf(claims.userId()).equals(sessionUserId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌已失效，请重新登录");
        }

        stringRedisTemplate.delete(refreshKey);
        stringRedisTemplate.opsForSet().remove(USER_SESSION_KEY_PREFIX + claims.userId(), claims.tokenId());
        return issueTokenPair(claims.userId(), claims.rememberMe());
    }

    @Override
    public void logoutAccessToken(String accessToken) {
        TokenClaims claims = parseAccessToken(accessToken);
        long remainingSeconds = Math.max(1L, claims.expiresAt() - Instant.now().getEpochSecond());
        stringRedisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + claims.tokenId(),
                String.valueOf(claims.userId()),
                Duration.ofSeconds(remainingSeconds)
        );
    }

    @Override
    public int kickoutOtherSessions(Long userId, String currentRefreshToken) {
        TokenClaims currentClaims = parseRefreshToken(currentRefreshToken);
        if (!userId.equals(currentClaims.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能踢出自己的登录会话");
        }

        String sessionKey = USER_SESSION_KEY_PREFIX + userId;
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(sessionKey);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }

        int kickedCount = 0;
        for (String sessionId : sessionIds) {
            if (sessionId.equals(currentClaims.tokenId())) {
                continue;
            }
            stringRedisTemplate.delete(REFRESH_KEY_PREFIX + sessionId);
            stringRedisTemplate.opsForSet().remove(sessionKey, sessionId);
            kickedCount++;
        }
        return kickedCount;
    }

    @Override
    public int invalidateAllRefreshSessions(Long userId) {
        String sessionKey = USER_SESSION_KEY_PREFIX + userId;
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(sessionKey);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }

        int removedCount = 0;
        for (String sessionId : sessionIds) {
            stringRedisTemplate.delete(REFRESH_KEY_PREFIX + sessionId);
            removedCount++;
        }
        stringRedisTemplate.delete(sessionKey);
        return removedCount;
    }

    private void saveRefreshSession(Long userId, String tokenId, long expiresIn) {
        String sessionKey = USER_SESSION_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + tokenId, String.valueOf(userId), Duration.ofSeconds(expiresIn));
        stringRedisTemplate.opsForSet().add(sessionKey, tokenId);
        stringRedisTemplate.expire(sessionKey, Duration.ofSeconds(expiresIn));
    }

    private String createToken(Long userId, String tokenType, long issuedAt, long expiresAt, boolean rememberMe) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        Map<String, Object> payload = new HashMap<>();
        payload.put("jti", UUID.randomUUID().toString().replace("-", ""));
        payload.put("sub", String.valueOf(userId));
        payload.put("tokenType", tokenType);
        payload.put("iat", issuedAt);
        payload.put("exp", expiresAt);
        payload.put("rememberMe", rememberMe);

        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String body = base64UrlEncode(writePayload(payload).getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + body);
        return header + "." + body + "." + signature;
    }

    private TokenClaims parseAndValidate(String token, String expectedTokenType) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录令牌格式错误");
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录令牌签名无效");
        }

        Map<String, Object> payload = readPayload(parts[1]);
        String tokenType = String.valueOf(payload.get("tokenType"));
        if (!expectedTokenType.equals(tokenType)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录令牌类型错误");
        }

        long expiresAt = numberAsLong(payload.get("exp"));
        if (expiresAt <= Instant.now().getEpochSecond()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已过期，请重新登录");
        }

        return new TokenClaims(
                String.valueOf(payload.get("jti")),
                Long.valueOf(String.valueOf(payload.get("sub"))),
                tokenType,
                numberAsLong(payload.get("iat")),
                expiresAt,
                Boolean.parseBoolean(String.valueOf(payload.get("rememberMe")))
        );
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            return base64UrlEncode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "令牌签名失败");
        }
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "令牌载荷序列化失败");
        }
    }

    private Map<String, Object> readPayload(String encodedPayload) {
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            return objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录令牌载荷无效");
        }
    }

    private long numberAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
