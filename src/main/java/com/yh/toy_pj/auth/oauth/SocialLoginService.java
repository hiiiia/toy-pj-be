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
        User user = userRepository.save(User.createSocial(info.name(), info.email()));
        socialAccountRepository.save(SocialAccount.create(user, info.provider(), info.providerUserId()));
        return user;
    }
}
