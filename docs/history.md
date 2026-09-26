# 개선 이력 (기존 프로젝트 → 현재)

| 기존 | 개선 |
|---|---|
| Gemini API 키가 `application.properties` 에 하드코딩 | 환경변수로 이동, `.gitignore` 에 `.env` 추가 |
| Controller 가 Repository 직접 호출, 필드 주입(`@Autowired`) | Controller → Service → Repository 계층 분리, 생성자 주입 |
| 엔티티를 요청/응답에 그대로 사용 (`@Data`) | record DTO + 검증 애너테이션, 엔티티는 도메인 메서드만 공개 |
| 상태값이 한글 문자열 (`"사용중"`, `"접수대기"`) | enum + 코드 API 로 라벨 제공 |
| 존재하지 않는 ID 수정 시 500 (`RuntimeException`) | `404 A001`, `404 T001` 등 의미 있는 에러 코드 |
| Gemini 요청 JSON 을 문자열 연결로 생성 (입력에 `"` 포함 시 깨짐) | 객체 직렬화, 헤더 인증, 타임아웃, 구조화된 JSON 응답 |
| 테스트 1개 (`contextLoads`) | 166개 (단위/슬라이스/통합, 실제 PostgreSQL), CI 자동 실행 |
| `User` 엔티티만 있고 사용되지 않음 | 요청자/담당자/자산 배정자로 실제 연관관계 사용 |
| 인증 없음 (누구나 모든 API 호출, 요청자 ID 를 본문으로 받음) | Spring Security + JWT, 역할별 권한, 요청자는 토큰에서 결정 |
| `ddl-auto=update` 로 Hibernate 가 테이블 자동 변경 | Flyway 마이그레이션 + `validate` |
| 로컬에서만 실행 가능 | Docker Compose 로 DB·백엔드·프론트엔드 한 번에 실행 |
| 무차별 대입에 무방비, 비밀번호 변경 불가 | 5회 실패 잠금, 비밀번호 변경·관리자 초기화 |
| 요청자와 담당자가 소통할 방법 없음 | 댓글·내부 메모·첨부파일 |
| 기한이 지나도 아무도 모름 | SLA 임박/초과 알림 (앱 + Slack) |
| 로그로 요청을 추적하기 어려움 | 요청 ID 로그 + 에러 응답에 요청 ID |

### 프론트엔드 연동 시 변경점
> 프론트엔드(yh-fe)는 아래 변경을 모두 반영했습니다. 개발 서버의 `/api` 프록시로 연결됩니다.

- 상태값: 한글 문자열 → 영문 코드 (`IN_USE`, `OPEN` 등). 라벨은 `GET /api/codes` 사용
- 목록 API: 배열 → 페이지 객체 (`content`, `totalElements`, `hasNext` ...)
- 대시보드: `/api/dashboard/stats` → `/api/dashboard/summary`
- 자산 수정: `PUT` → `PATCH` (부분 수정), 배정/반납 등은 별도 엔드포인트
- 티켓 삭제 → `PATCH /status` 로 `CANCELED` 처리 (이력 보존)
- AI 챗: `/api/gemini/chat` `{prompt}` → `/api/ai/chat` `{message}` → `{answer}`


---
[← README](../README.md)
