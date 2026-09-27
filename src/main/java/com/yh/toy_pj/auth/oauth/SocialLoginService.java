package com.yh.toy_pj.auth.oauth;

import com.yh.toy_pj.domain.user.SocialAccount;
import com.yh.toy_pj.domain.user.SocialAccountRepository;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRepository;
import com.yh.toy_pj.global.error.BusinessException;
import com.yh.toy_pj.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SNS 사용자 정보로 우리 회원을 찾고, 없으면 가입시킨다. */
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    /** users.name 컬럼 길이 (V1) */
    private static final int NAME_MAX_LENGTH = 50;

    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;

    @Transactional
    public User loginOrSignup(OAuthUserInfo info) {
        return socialAccountRepository.findByProviderAndProviderUserId(info.provider(), info.providerUserId())
                .map(SocialAccount::getUser)      // 이미 연결된 SNS 계정이면 그 회원
                .orElseGet(() -> signup(info));   // 처음이면 가입
    }

    /** 처음 들어온 SNS 계정 → 회원 생성 + 연결 정보 저장. 이메일 없음/이미 가입된 이메일이면 거부(자동 연동하지 않음) */
    private User signup(OAuthUserInfo info) {
        if (info.email() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_REQUIRED);
        }
        if (userRepository.findByEmail(User.normalizeEmail(info.email())).isPresent()) {
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_ALREADY_REGISTERED);
        }
        User user = userRepository.save(User.createSocial(displayName(info), info.email()));
        socialAccountRepository.save(SocialAccount.create(user, info.provider(), info.providerUserId()));
        return user;
    }

    /**
     * 이름은 제공자에 따라 비어 있거나(동의 거부, 미설정) users.name(50자)보다 길 수 있다.
     * 비어 있으면 이메일 앞부분을 쓰고, 길면 잘라서 가입 자체가 실패(500)하지 않게 한다.
     */
    static String displayName(OAuthUserInfo info) {
        String name = info.name() != null ? info.name() : info.email().substring(0, info.email().indexOf('@'));
        return name.length() > NAME_MAX_LENGTH ? name.substring(0, NAME_MAX_LENGTH) : name;
    }
}
