package com.opsdesk.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsdesk.auth.model.TokenClaims;
import com.opsdesk.auth.model.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JWT 令牌服务测试。
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        tokenService = new TokenServiceImpl(stringRedisTemplate, new ObjectMapper(), "unit-test-secret");
    }

    @Test
    void issueTokenPairShouldCreateRefreshSessionAndParseAccessToken() {
        TokenPair tokenPair = tokenService.issueTokenPair(1001L, true);

        TokenClaims claims = tokenService.parseAccessToken(tokenPair.accessToken());

        assertThat(claims.userId()).isEqualTo(1001L);
        assertThat(claims.tokenType()).isEqualTo("ACCESS");
        assertThat(tokenPair.refreshExpiresIn()).isEqualTo(30L * 24 * 60 * 60);
        verify(valueOperations).set(startsWith("auth:refresh:"), eq("1001"), any(Duration.class));
        verify(setOperations).add(eq("auth:user:sessions:1001"), anyString());
    }

    @Test
    void logoutAccessTokenShouldWriteAccessTokenBlacklist() {
        TokenPair tokenPair = tokenService.issueTokenPair(1002L, false);

        tokenService.logoutAccessToken(tokenPair.accessToken());

        verify(valueOperations).set(startsWith("auth:blacklist:"), eq("1002"), any(Duration.class));
    }
}
