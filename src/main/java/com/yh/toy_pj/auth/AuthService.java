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
import java.time.Duration;
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
    private final AuthPolicyProperties policy;
    private final Clock clock;

    /** 존재하지 않는 이메일로 로그인할 때도 해시 비교 시간을 동일하게 소모시키기 위한 더미 해시 (최초 사용 시 1번만 생성) */
    private volatile String dummyHash;

    /** 로그인 결과: 응답 본문(access token)과 쿠키로 내려줄 refresh token 원문 */
    public record LoginResult(TokenResponse body, String refreshToken) {
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(User.normalizeEmail(request.email()))) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = User.create(request.name(), request.email(), passwordEncoder.encode(request.password()),
                request.department(), UserRole.USER);
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * 로그인. 연속으로 {@code maxLoginAttempts} 번 실패하면 {@code lockDuration} 동안 잠근다.
     *
     * noRollbackFor: 비밀번호가 틀려 예외를 던지더라도 "실패 횟수 증가"는 DB 에 저장되어야 한다.
     * (기본 설정이면 RuntimeException 발생 시 트랜잭션 전체가 롤백되어 실패 횟수가 영원히 0 으로 남는다.)
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResult login(String email, String rawPassword) {
        LocalDateTime now = now();
        User user = userRepository.findByEmail(User.normalizeEmail(email)).orElse(null);
        if (user == null || !user.hasPassword()) {
            // 이메일 존재 여부(또는 SNS 전용 계정 여부)를 응답 시간 차이로 추측하지 못하도록 같은 비용의 비교를 수행한다.
            // SNS 로만 가입한 계정은 비밀번호가 없으므로 이메일 로그인은 항상 실패한다. (그대로 두면 null 해시 비교로 500)
            passwordEncoder.matches(rawPassword, dummyHash());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.isLocked(now)) {
            throw lockedException(user, now);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            boolean locked = user.recordLoginFailure(now, policy.maxLoginAttempts(), policy.lockDuration());
            if (locked) {
                log.warn("로그인 {}회 연속 실패로 계정 잠금: userId={}", policy.maxLoginAttempts(), user.getId());
                throw lockedException(user, now);
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.recordLoginSuccess();
        log.info("로그인 성공: userId={}", user.getId());
        return issueTokens(user);
    }

    /**
     * 본인 비밀번호 변경. 현재 비밀번호를 다시 확인하고,
     * 다른 기기의 로그인 세션(refresh token)을 모두 폐기한 뒤 지금 기기에만 새 토큰을 발급한다.
     */
    @Transactional
    public LoginResult changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!user.hasPassword()) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_WITHOUT_PASSWORD);
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(newPassword), now());
        int revoked = refreshTokenRepository.deleteAllByUserId(user.getId());
        log.info("비밀번호 변경: userId={}, 폐기한 세션 {}개", user.getId(), revoked);
        return issueTokens(user);
    }

    private BusinessException lockedException(User user, LocalDateTime now) {
        long seconds = Duration.between(now, user.getLockedUntil()).toSeconds();
        long minutes = Math.max(1, (seconds + 59) / 60); // 남은 시간을 분 단위로 올림
        return new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                "로그인에 %d회 연속 실패해 계정이 잠겼습니다. 약 %d분 후 다시 시도하거나 IT 관리자에게 문의하세요."
                        .formatted(policy.maxLoginAttempts(), minutes));
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
        // 같은 refresh token 으로 동시에 두 번 요청하면 둘 다 조회에 성공할 수 있다.
        // 삭제한 행 수로 "먼저 삭제한 요청만" 통과시켜 토큰 한 개로 새 토큰이 두 쌍 발급되지 않게 한다.
        if (refreshTokenRepository.deleteByIdAndCount(saved.getId()) == 0) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokens(saved.getUser());
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

    @Transactional
    public LoginResult socialLogin(User user) {
        log.info("SNS 로그인 성공: userId={}", user.getId());
        return issueTokens(user);
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            // 동시에 여러 요청이 들어와 두 번 만들어져도 결과는 같은 용도의 해시라 문제없다 (volatile 로 가시성만 보장)
            hash = passwordEncoder.encode("dummy-password-for-timing");
            dummyHash = hash;
        }
        return hash;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
