package com.yh.toy_pj.global.security;

import com.yh.toy_pj.auth.jwt.JwtAuthenticationFilter;
import com.yh.toy_pj.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 로그인은 했지만 권한이 없는 요청 (예: 일반 사용자가 관리자 API 호출, 임시 비밀번호 상태) → 403 + 공통 에러 포맷 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean passwordChangeRequired = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + JwtAuthenticationFilter.PASSWORD_CHANGE_REQUIRED));
        errorWriter.write(response, passwordChangeRequired ? ErrorCode.PASSWORD_CHANGE_REQUIRED : ErrorCode.FORBIDDEN);
    }
}
