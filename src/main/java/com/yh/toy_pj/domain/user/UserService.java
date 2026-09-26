package com.yh.toy_pj.domain.user;

import com.yh.toy_pj.auth.RefreshTokenRepository;
import com.yh.toy_pj.auth.TemporaryPasswordGenerator;
import com.yh.toy_pj.domain.user.dto.PasswordResetResponse;
import com.yh.toy_pj.domain.user.dto.UserCreateRequest;
import com.yh.toy_pj.domain.user.dto.UserResponse;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final Clock clock;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(User.normalizeEmail(request.email()))) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = User.create(request.name(), request.email(), passwordEncoder.encode(request.password()),
                request.department(), request.role());
        return UserResponse.from(userRepository.save(user));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    public UserResponse get(Long id) {
        return UserResponse.from(getUser(id));
    }

    /**
     * 관리자가 사용자의 비밀번호를 임시 비밀번호로 초기화한다.
     * 잠금이 풀리고 기존 로그인 세션은 모두 폐기되며, 사용자는 다음 로그인 후 비밀번호를 바꾸도록 안내받는다.
     */
    @Transactional
    public PasswordResetResponse resetPassword(Long id) {
        User user = getUser(id);
        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.resetPassword(passwordEncoder.encode(temporaryPassword), LocalDateTime.now(clock));
        refreshTokenRepository.deleteAllByUserId(user.getId());
        log.info("비밀번호 초기화: userId={}", user.getId());
        return new PasswordResetResponse(user.getId(), temporaryPassword);
    }

    /** 관리자가 잠긴 계정을 즉시 해제한다. */
    @Transactional
    public UserResponse unlock(Long id) {
        User user = getUser(id);
        user.unlock();
        return UserResponse.from(user);
    }

    /** 다른 도메인 서비스에서 사용자 엔티티가 필요할 때 사용 (존재하지 않으면 USER_NOT_FOUND) */
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다. id=" + id));
    }
}
