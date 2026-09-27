package com.yh.toy_pj.auth.oauth;

import com.yh.toy_pj.domain.user.SocialProvider;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * SNS 제공자마다 다른 사용자 정보 응답을 하나의 모양으로 통일한다.
 * 여기서는 값만 옮기고, "이메일이 없으면 가입 거부" 같은 정책은 SocialLoginService 가 정한다.
 */
public record OAuthUserInfo(SocialProvider provider, String providerUserId, String name, String email) {

    /** compact 생성자: record 의 필드가 채워지기 직전에 실행되는 검사 */
    public OAuthUserInfo {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(providerUserId, "providerUserId");
    }

    /**
     * @param registrationId application.properties 의 registration 이름 ("google" / "kakao" / "naver")
     * @param attributes     Spring 이 제공자의 사용자 정보 API 에서 받아온 원본 응답
     */
    public static OAuthUserInfo of(String registrationId, Map<String, Object> attributes) {
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase(Locale.ROOT)); // 모르는 값이면 IllegalArgumentException
        return switch (provider) {
            case GOOGLE -> ofGoogle(attributes);
            case KAKAO -> ofKakao(attributes);
            case NAVER -> ofNaver(attributes);
        };
    }

    /** { "sub": "...", "name": "...", "email": "...", "email_verified": true } */
    private static OAuthUserInfo ofGoogle(Map<String, Object> attrs) {
        String email = verifiedOnly(text(attrs, "email"), attrs.get("email_verified"));
        return new OAuthUserInfo(SocialProvider.GOOGLE, text(attrs, "sub"), text(attrs, "name"), email);
    }

    /** { "id": 123, "kakao_account": { "email": "...", "is_email_verified": true, "profile": { "nickname": "..." } } } */
    private static OAuthUserInfo ofKakao(Map<String, Object> attrs) {
        Map<String, Object> account = child(attrs, "kakao_account");
        Map<String, Object> profile = child(account, "profile");
        String email = verifiedOnly(text(account, "email"), account.get("is_email_verified"));
        return new OAuthUserInfo(SocialProvider.KAKAO, text(attrs, "id"), text(profile, "nickname"), email);
    }

    /** { "resultcode": "00", "response": { "id": "...", "email": "...", "name": "..." } } */
    private static OAuthUserInfo ofNaver(Map<String, Object> attrs) {
        Map<String, Object> response = child(attrs, "response");
        return new OAuthUserInfo(SocialProvider.NAVER, text(response, "id"), text(response, "name"), text(response, "email"));
    }

    // ===== 파싱 도우미 =====

    /** 값을 문자열로 꺼낸다. 없거나 빈 문자열이면 null. (카카오 id 처럼 숫자로 오는 값도 문자열로 바뀐다) */
    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** 중첩된 객체를 꺼낸다. 없으면 빈 Map 을 돌려줘서 다음 .get() 에서 NullPointerException 이 나지 않게 한다. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    /** 제공자가 "인증되지 않은 이메일"이라고 명시한 경우 버린다. 남의 이메일로 가입하는 것을 막기 위함. */
    private static String verifiedOnly(String email, Object verifiedFlag) {
        return Boolean.FALSE.equals(verifiedFlag) ? null : email;
    }
}
