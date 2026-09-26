package com.yh.toy_pj.auth;

import com.yh.toy_pj.auth.dto.LoginRequest;
import com.yh.toy_pj.auth.dto.PasswordChangeRequest;
import com.yh.toy_pj.auth.dto.SignupRequest;
import com.yh.toy_pj.auth.dto.TokenResponse;
import com.yh.toy_pj.auth.jwt.JwtProperties;
import com.yh.toy_pj.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Refresh token 은 JavaScript 에서 읽을 수 없는 HttpOnly 쿠키로 내려준다.
 * - XSS 로 스크립트가 주입되어도 refresh token 은 탈취할 수 없다.
 * - SameSite=Strict + Path=/api/auth 로 다른 사이트의 요청이나 다른 API 호출에는 쿠키가 붙지 않는다.
 * access token 은 응답 본문으로 주고, 프론트엔드는 메모리(변수)에만 보관한다.
 */
@Tag(name = "Auth", description = "로그인 / 토큰 재발급 / 로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "회원가입", description = "일반 사용자(USER)로 가입한다.")
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @Operation(summary = "로그인", description = "access token 은 본문으로, refresh token 은 HttpOnly 쿠키로 발급한다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(authService.login(request.email(), request.password()));
    }

    @Operation(summary = "토큰 재발급", description = "refresh token 쿠키로 새 access token 을 발급한다. 사용한 refresh token 은 폐기된다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return withRefreshCookie(authService.refresh(refreshToken));
    }

    @Operation(summary = "로그아웃", description = "refresh token 을 폐기하고 쿠키를 지운다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 변경한다. 다른 기기의 로그인은 모두 해제되고, 지금 기기에는 새 토큰을 발급한다.")
    @PatchMapping("/password")
    public ResponseEntity<TokenResponse> changePassword(@Valid @RequestBody PasswordChangeRequest request,
                                                        @AuthenticationPrincipal AuthUser me) {
        return withRefreshCookie(authService.changePassword(me.id(), request.currentPassword(), request.newPassword()));
    }

    @Operation(summary = "내 정보")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return authService.me(me.id());
    }

    private ResponseEntity<TokenResponse> withRefreshCookie(AuthService.LoginResult result) {
        ResponseCookie cookie = refreshCookie(result.refreshToken(), jwtProperties.refreshTokenTtl());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.body());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
