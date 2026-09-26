package com.yh.toy_pj.auth;

import com.yh.toy_pj.auth.dto.SignupRequest;
import com.yh.toy_pj.auth.dto.TokenResponse;
import com.yh.toy_pj.auth.jwt.JwtProperties;
import com.yh.toy_pj.auth.jwt.JwtTokenProvider;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRepository;
import com.yh.toy_pj.domain.user.UserRole;
import com.yh.toy_pj.domain.user.dto.UserResponse;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 흐름
 * <pre>
 * 1. 로그인      : 이메일/비밀번호 확인 → access token(JWT, 30분) + refresh token(무작위 값, 14일) 발급
 * 2. API 호출    : Authorization: Bearer {access token}
 * 3. access 만료 : refresh token(쿠키)으로 /api/auth/refresh 호출 → 새 토큰 한 쌍 발급 (기존 refresh token 폐기 = rotation)
 * 4. 로그아웃    : refresh token 삭제
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    /** 존재하지 않는 이메일로 로그인할 때도 해시 비교 시간을 동일하게 소모시키기 위한 더미 해시 */
    private String dummyHash;

    /** 로그인 결과: 응답 본문(access token)과 쿠키로 내려줄 refresh token 원문 */
    public record LoginResult(TokenResponse body, String refreshToken) {
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = User.create(request.name(), request.email(), passwordEncoder.encode(request.password()),
                request.department(), UserRole.USER);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public LoginResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // 이메일 존재 여부를 응답 시간 차이로 추측하지 못하도록 같은 비용의 비교를 수행한다.
            passwordEncoder.matches(rawPassword, dummyHash());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        log.info("로그인 성공: userId={}", user.getId());
        return issueTokens(user);
    }

    /** Refresh token rotation: 사용한 refresh token 은 즉시 폐기하고 새 토큰을 발급한다. */
    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        RefreshToken saved = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (saved.isExpired(now())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        User user = saved.getUser();
        refreshTokenRepository.delete(saved);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawRefreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    public UserResponse me(Long userId) {
        return userRepository.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private LoginResult issueTokens(User user) {
        String accessToken = tokenProvider.createAccessToken(user);
        String refreshToken = TokenHasher.newToken();
        refreshTokenRepository.save(new RefreshToken(user, TokenHasher.sha256(refreshToken),
                now().plus(jwtProperties.refreshTokenTtl())));
        TokenResponse body = TokenResponse.bearer(accessToken, tokenProvider.accessTokenTtlSeconds(), UserResponse.from(user));
        return new LoginResult(body, refreshToken);
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("dummy-password-for-timing");
        }
        return dummyHash;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
