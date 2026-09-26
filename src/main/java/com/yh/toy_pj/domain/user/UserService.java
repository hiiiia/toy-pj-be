package com.yh.toy_pj.domain.user;

import com.yh.toy_pj.domain.user.dto.UserCreateRequest;
import com.yh.toy_pj.domain.user.dto.UserResponse;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = User.create(request.name(), request.email(), request.department(), request.role());
        return UserResponse.from(userRepository.save(user));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    public UserResponse get(Long id) {
        return UserResponse.from(getUser(id));
    }

    /** 다른 도메인 서비스에서 사용자 엔티티가 필요할 때 사용 (존재하지 않으면 USER_NOT_FOUND) */
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다. id=" + id));
    }
}
