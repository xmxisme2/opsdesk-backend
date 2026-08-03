package com.opsdesk.ai.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.ai.config.AiServiceProperties;
import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主应用 Service JWT 签发器测试。
 */
class ServiceJwtTokenIssuerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldIssueShortLivedServiceTokenWithUserContext() throws Exception {
        AiServiceProperties properties = properties("opsdesk-ai-service-test-secret-32chars");
        ServiceJwtTokenIssuer issuer = new ServiceJwtTokenIssuer(objectMapper, properties);
        CurrentUser user = new CurrentUser(
                1001L,
                "13800000000",
                "admin",
                List.of("ADMIN"),
                List.of()
        );

        String token = issuer.issue(user);
        String[] parts = token.split("\\.");
        Map<String, Object> payload = objectMapper.readValue(
                Base64.getUrlDecoder().decode(parts[1]),
                new TypeReference<>() {
                }
        );

        assertEquals(3, parts.length);
        assertEquals("SERVICE", payload.get("tokenType"));
        assertEquals("opsdesk-backend", payload.get("service"));
        assertEquals("1001", payload.get("userId"));
        assertTrue(((Number) payload.get("exp")).longValue() - ((Number) payload.get("iat")).longValue() <= 60L);
    }

    @Test
    void shouldFailSafelyWhenSecretIsMissing() {
        ServiceJwtTokenIssuer issuer = new ServiceJwtTokenIssuer(objectMapper, properties(""));

        BusinessException exception = assertThrows(BusinessException.class, () -> issuer.issue(null));

        assertEquals(ErrorCode.AI_SERVICE_UNAVAILABLE, exception.getErrorCode());
    }

    private AiServiceProperties properties(String secret) {
        AiServiceProperties properties = new AiServiceProperties();
        properties.setServiceJwtSecret(secret);
        return properties;
    }
}
