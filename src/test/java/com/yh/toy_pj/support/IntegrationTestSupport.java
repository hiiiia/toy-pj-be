package com.yh.toy_pj.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yh.toy_pj.domain.user.User;
import com.yh.toy_pj.domain.user.UserRepository;
import com.yh.toy_pj.domain.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 테스트 공통 기반: 실제 스프링 컨텍스트 + PostgreSQL 컨테이너(Flyway 로 스키마 생성) + 실제 보안 필터.
 * 각 테스트는 트랜잭션 안에서 실행되고 끝나면 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainer.class)
@Transactional
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "password1";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** 관리자는 회원가입으로 만들 수 없으므로 저장소에 직접 생성한다. */
    protected User createUser(String name, String email, UserRole role) {
        return userRepository.save(User.create(name, email, passwordEncoder.encode(PASSWORD), "테스트팀", role));
    }

    /** 로그인해서 "Bearer {access token}" 헤더 값을 돌려준다. */
    protected String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    protected ResultActions postJson(String url, String token, String body) throws Exception {
        return mockMvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions patchJson(String url, String token, String body) throws Exception {
        return mockMvc.perform(patch(url).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected long idOf(ResultActions actions) throws Exception {
        Number id = JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
