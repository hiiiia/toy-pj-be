package com.yh.toy_pj.domain.user;

import com.yh.toy_pj.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users") // PostgreSQL 에서 user 는 예약어
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt 해시. 원문 비밀번호는 어디에도 저장하지 않는다. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 50)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** 연속 로그인 실패 횟수. 성공하거나 잠기면 0 으로 초기화된다. */
    @Column(nullable = false)
    private int failedLoginCount;

    /** 이 시각 전까지 로그인 불가 (무차별 대입 공격 방지) */
    private LocalDateTime lockedUntil;

    private LocalDateTime passwordChangedAt;

    /** 관리자가 임시 비밀번호로 초기화한 상태. 로그인 후 비밀번호를 바꾸도록 안내한다. */
    @Column(nullable = false)
    private boolean mustChangePassword;

    private User(String name, String email, String encodedPassword, String department, UserRole role) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.password = encodedPassword;
        this.department = department;
        this.role = role;
    }

    /**
     * @param encodedPassword 반드시 PasswordEncoder 로 해시한 값을 넘긴다.
     */
    public static User create(String name, String email, String encodedPassword, String department, UserRole role) {
        return new User(name, email, encodedPassword, department, role);
    }

    /**
     * 이메일은 대소문자를 구분하지 않는다(Hong@x.com 과 hong@x.com 은 같은 주소).
     * 저장·조회 모두 이 형태로 맞춰야 중복 가입과 "대문자로 입력하면 로그인 실패"를 막을 수 있다.
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    // ===== 로그인 시도 제한 =====

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /**
     * 로그인 실패를 기록한다. 연속 실패가 maxAttempts 에 도달하면 lockDuration 동안 잠근다.
     *
     * @return 이번 실패로 계정이 잠겼는지 여부
     */
    public boolean recordLoginFailure(LocalDateTime now, int maxAttempts, Duration lockDuration) {
        failedLoginCount++;
        if (failedLoginCount >= maxAttempts) {
            lockedUntil = now.plus(lockDuration);
            failedLoginCount = 0;
            return true;
        }
        return false;
    }

    public void recordLoginSuccess() {
        failedLoginCount = 0;
        lockedUntil = null;
    }

    public void unlock() {
        recordLoginSuccess();
    }

    // ===== 비밀번호 =====

    /** 본인이 비밀번호를 변경한다. */
    public void changePassword(String encodedPassword, LocalDateTime now) {
        this.password = encodedPassword;
        this.passwordChangedAt = now;
        this.mustChangePassword = false;
    }

    /** 관리자가 임시 비밀번호로 초기화한다. 잠금도 함께 풀고, 다음 로그인 때 변경을 요구한다. */
    public void resetPassword(String encodedTemporaryPassword, LocalDateTime now) {
        this.password = encodedTemporaryPassword;
        this.passwordChangedAt = now;
        this.mustChangePassword = true;
        unlock();
    }
}
