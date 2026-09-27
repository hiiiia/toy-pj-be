package com.yh.toy_pj.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yh.toy_pj.domain.user.SocialProvider;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 제공자별 실제 응답 형태(Map)를 넣었을 때 공통 모양으로 올바르게 바뀌는지 검증한다.
 * Spring 이 JSON 을 Map 으로 바꿔서 넘겨주므로, 테스트도 JSON 대신 Map.of(...) 로 같은 구조를 만든다.
 */
class OAuthUserInfoTest {

    @Nested
    @DisplayName("Google")
    class Google {

        @Test
        @DisplayName("최상위의 sub / name / email 을 꺼낸다")
        void parsesTopLevelFields() {
            // { "sub": "109876543210", "name": "홍길동", "email": "hong@gmail.com", "email_verified": true }
            Map<String, Object> attrs = Map.of(
                    "sub", "109876543210",
                    "name", "홍길동",
                    "email", "hong@gmail.com",
                    "email_verified", true);

            OAuthUserInfo info = OAuthUserInfo.of("google", attrs);

            assertThat(info.provider()).isEqualTo(SocialProvider.GOOGLE);
            assertThat(info.providerUserId()).isEqualTo("109876543210");
            assertThat(info.name()).isEqualTo("홍길동");
            assertThat(info.email()).isEqualTo("hong@gmail.com");
        }

        @Test
        @DisplayName("인증되지 않은 이메일(email_verified=false)은 버린다")
        void dropsUnverifiedEmail() {
            Map<String, Object> attrs = Map.of(
                    "sub", "109876543210",
                    "name", "홍길동",
                    "email", "hong@gmail.com",
                    "email_verified", false);

            assertThat(OAuthUserInfo.of("google", attrs).email()).isNull();
        }
    }

    @Nested
    @DisplayName("Kakao")
    class Kakao {

        @Test
        @DisplayName("숫자 id 를 문자열로 바꾸고, 중첩된 kakao_account / profile 에서 값을 꺼낸다")
        void parsesNestedFields() {
            // { "id": 3812345678, "kakao_account": { "email": "...", "is_email_verified": true, "profile": { "nickname": "길동이" } } }
            Map<String, Object> attrs = Map.of(
                    "id", 3812345678L,
                    "kakao_account", Map.of(
                            "email", "hong@kakao.com",
                            "is_email_verified", true,
                            "profile", Map.of("nickname", "길동이")));

            OAuthUserInfo info = OAuthUserInfo.of("kakao", attrs);

            assertThat(info.provider()).isEqualTo(SocialProvider.KAKAO);
            assertThat(info.providerUserId()).isEqualTo("3812345678");
            assertThat(info.name()).isEqualTo("길동이");
            assertThat(info.email()).isEqualTo("hong@kakao.com");
        }

        @Test
        @DisplayName("이메일 제공에 동의하지 않으면(email 키 없음) 예외 없이 email 이 null 이다")
        void emailConsentDenied() {
            Map<String, Object> attrs = Map.of(
                    "id", 3812345678L,
                    "kakao_account", Map.of("profile", Map.of("nickname", "길동이")));

            OAuthUserInfo info = OAuthUserInfo.of("kakao", attrs);

            assertThat(info.email()).isNull();
            assertThat(info.name()).isEqualTo("길동이");
        }

        @Test
        @DisplayName("kakao_account 자체가 없어도 NullPointerException 없이 id 만 채운다")
        void missingAccount() {
            OAuthUserInfo info = OAuthUserInfo.of("kakao", Map.of("id", 3812345678L));

            assertThat(info.providerUserId()).isEqualTo("3812345678");
            assertThat(info.name()).isNull();
            assertThat(info.email()).isNull();
        }

        @Test
        @DisplayName("인증되지 않은 이메일(is_email_verified=false)은 버린다")
        void dropsUnverifiedEmail() {
            Map<String, Object> attrs = Map.of(
                    "id", 3812345678L,
                    "kakao_account", Map.of(
                            "email", "someone-else@kakao.com",
                            "is_email_verified", false));

            assertThat(OAuthUserInfo.of("kakao", attrs).email()).isNull();
        }
    }

    @Nested
    @DisplayName("Naver")
    class Naver {

        @Test
        @DisplayName("response 안의 id / name / email 을 꺼낸다")
        void parsesResponseFields() {
            // { "resultcode": "00", "message": "success", "response": { "id": "AbCdEf123", "email": "...", "name": "홍길동" } }
            Map<String, Object> attrs = Map.of(
                    "resultcode", "00",
                    "message", "success",
                    "response", Map.of(
                            "id", "AbCdEf123",
                            "email", "hong@naver.com",
                            "name", "홍길동"));

            OAuthUserInfo info = OAuthUserInfo.of("naver", attrs);

            assertThat(info.provider()).isEqualTo(SocialProvider.NAVER);
            assertThat(info.providerUserId()).isEqualTo("AbCdEf123");
            assertThat(info.name()).isEqualTo("홍길동");
            assertThat(info.email()).isEqualTo("hong@naver.com");
        }
    }

    @Nested
    @DisplayName("공통")
    class Common {

        @Test
        @DisplayName("registrationId 는 대소문자를 구분하지 않는다")
        void registrationIdIsCaseInsensitive() {
            OAuthUserInfo info = OAuthUserInfo.of("GOOGLE", Map.of("sub", "1"));

            assertThat(info.provider()).isEqualTo(SocialProvider.GOOGLE);
        }

        @Test
        @DisplayName("지원하지 않는 제공자면 예외가 난다")
        void unknownProvider() {
            assertThatThrownBy(() -> OAuthUserInfo.of("github", Map.of("id", "1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("사용자 식별자(id)가 없으면 예외가 난다 - 누구인지 구분할 수 없으므로 가입시키지 않는다")
        void missingProviderUserId() {
            assertThatThrownBy(() -> OAuthUserInfo.of("naver", Map.of("response", Map.of("name", "홍길동"))))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("providerUserId");
        }

        @Test
        @DisplayName("빈 문자열 값은 null 로 취급한다")
        void blankValueIsNull() {
            OAuthUserInfo info = OAuthUserInfo.of("google", Map.of("sub", "1", "name", "  ", "email", ""));

            assertThat(info.name()).isNull();
            assertThat(info.email()).isNull();
        }
    }
}
