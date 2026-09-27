# API 요약

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입 (USER) |
| GET | `/oauth2/authorization/{google\|kakao\|naver}` | SNS 로그인 시작 (페이지 이동). 성공 시 refresh 쿠키 + `/oauth/callback`, 실패 시 `/login?error=AUTH009\|010\|011` |
| GET | `/login/oauth2/code/{provider}` | 제공자 콜백 (Spring Security 가 처리, 각 개발자 콘솔에 등록) |
| POST | `/api/auth/login` | 로그인 → access token + refresh token 쿠키 |
| POST | `/api/auth/refresh` | 토큰 재발급 (rotation) |
| POST | `/api/auth/logout` | 로그아웃 (refresh token 폐기) |
| GET | `/api/auth/me` | 내 정보 |
| PATCH | `/api/auth/password` | 비밀번호 변경 (다른 기기 로그인 해제) |
| POST | `/api/users` | 사용자 등록 (관리자 포함, ADMIN 전용) |
| GET | `/api/users`, `/api/users/{id}` | 사용자 조회 (ADMIN 전용) |
| POST | `/api/users/{id}/password-reset` · `/unlock` | 임시 비밀번호 발급 · 잠금 해제 (ADMIN 전용) |
| POST | `/api/assets` | 자산 등록 (재고 상태) |
| GET | `/api/assets?status=&type=&assignedUserId=&keyword=&page=&size=&sort=` | 자산 검색 |
| GET / PATCH / DELETE | `/api/assets/{id}` | 조회 / 부분 수정 / 삭제 |
| POST | `/api/assets/{id}/assign` · `/return` · `/repair` · `/repair/complete` · `/dispose` | 배정 · 반납 · 점검 · 점검완료 · 폐기 |
| POST | `/api/tickets` | 티켓 접수 (category/priority 생략 시 자동 분류) |
| GET | `/api/tickets?status=&priority=&category=&requesterId=&assigneeId=&unassigned=&keyword=` | 티켓 검색 |
| GET | `/api/tickets/{id}` | 상세 (처리 이력, 가능한 다음 상태 포함) |
| POST | `/api/tickets/{id}/assign` | 담당자 지정 (ADMIN 만) |
| PATCH | `/api/tickets/{id}/status` | 상태 변경 |
| PATCH | `/api/tickets/{id}/classification` | 재분류 (SLA 재계산) |
| GET / POST | `/api/tickets/{id}/comments` | 댓글 목록 / 작성 (`internal` = 내부 메모) |
| DELETE | `/api/tickets/{id}/comments/{commentId}` | 댓글 삭제 (작성자) |
| GET / POST | `/api/tickets/{id}/attachments` | 첨부 목록 / 업로드 (multipart `file`) |
| GET / DELETE | `/api/tickets/{id}/attachments/{attachmentId}` | 다운로드 / 삭제 |
| GET | `/api/notifications` | 내 최근 알림 30개 + 안 읽은 개수 |
| POST | `/api/notifications/{id}/read` · `/read-all` | 읽음 처리 |
| GET | `/api/dashboard/summary` | 대시보드 통계 |
| GET | `/api/codes` | 공통 코드 |
| POST | `/api/ai/triage` | 분류 미리보기 |
| POST | `/api/ai/chat` | IT 헬프데스크 챗봇 |

### 에러 응답 포맷

```json
{
  "code": "T002",
  "message": "'접수대기' 상태에서 '종료' 상태로 변경할 수 없습니다.",
  "status": 409,
  "errors": [ { "field": "title", "rejectedValue": "", "reason": "공백일 수 없습니다" } ],
  "requestId": "9685057a545293db",
  "timestamp": "2026-09-23T17:09:16.8384"
}
```
Swagger UI 우측 상단 **Authorize** 에 로그인 응답의 `accessToken` 을 넣으면 인증이 필요한 API 도 호출할 수 있습니다.

`requestId` 는 응답 헤더 `X-Request-Id` 와 같고, 서버 로그의 모든 줄에 `[9685057a545293db]` 형태로 찍힙니다. 사용자가 이 값을 알려주면 해당 요청의 로그만 바로 찾을 수 있습니다.

```
INFO [nio-8080-exec-1] [9685057a545293db] c.y.t.global.logging.RequestIdFilter : POST /api/tickets/1/attachments → 201 (23ms)
```

에러 코드 전체 목록: [`ErrorCode.java`](../src/main/java/com/yh/toy_pj/global/error/ErrorCode.java)

전체 명세는 서버 실행 후 Swagger UI(`/swagger-ui.html`)에서 확인할 수 있습니다.


---
[← README](../README.md)
