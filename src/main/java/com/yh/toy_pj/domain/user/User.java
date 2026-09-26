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

    private User(String name, String email, String encodedPassword, String department, UserRole role) {
        this.name = name;
        this.email = email;
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

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
