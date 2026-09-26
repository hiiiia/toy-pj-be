package com.yh.toy_pj.auth.jwt;

import com.yh.toy_pj.domain.user.AccountStatus;
import com.yh.toy_pj.domain.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 서명·만료 검증을 통과한 access token 이 "지금도" 쓸 수 있는지 DB 의 계정 상태와 비교한다.
 *
 * JWT 는 서버가 저장하지 않으므로 한 번 발급하면 만료(30분) 전까지 폐기할 방법이 없다.
 * 그래서 비밀번호가 바뀐 뒤(본인 변경, 관리자 초기화)에 발급된 토큰만 인정하고,
 * 임시 비밀번호 상태면 비밀번호 변경 외의 API 를 막는다.
 * 비용: 요청마다 사용자 PK 조회 1번 (컬럼 2개). 트래픽이 커지면 캐시(Redis 등)로 옮길 수 있다.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenVerifier {

    public enum Result { VALID, PASSWORD_CHANGE_REQUIRED, REVOKED }

    private final UserRepository userRepository;
    private final Clock clock;

    public Result verify(JwtTokenProvider.AccessToken token) {
        return userRepository.findAccountStatusById(token.user().id())
                .map(status -> verify(token.issuedAt(), status))
                .orElse(Result.REVOKED); // 삭제된 사용자
    }

    private Result verify(Instant issuedAt, AccountStatus status) {
        if (status.passwordChangedAt() != null) {
            // JWT 의 발급 시각(iat)은 초 단위라 비교 기준도 초 단위로 맞춘다.
            // (같은 초에 비밀번호를 바꾸고 새 토큰을 받은 경우 새 토큰이 거절되지 않도록)
            Instant changedAt = status.passwordChangedAt().atZone(clock.getZone()).toInstant().truncatedTo(ChronoUnit.SECONDS);
            if (issuedAt.isBefore(changedAt)) {
                return Result.REVOKED;
            }
        }
        return status.mustChangePassword() ? Result.PASSWORD_CHANGE_REQUIRED : Result.VALID;
    }
}
