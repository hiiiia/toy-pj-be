package com.yh.toy_pj.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /** 해당 사용자의 모든 로그인 세션 폐기 (비밀번호 변경·초기화 시) */
    @Modifying
    @Query("delete from RefreshToken r where r.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
