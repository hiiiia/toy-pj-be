# 테스트 전략

`./gradlew test` — **166개 테스트** (파라미터·반복 테스트 포함), 라인 커버리지 약 **82%** (JaCoCo). 실제 PostgreSQL 컨테이너 사용

| 레벨 | 대상 | 검증 내용 |
|---|---|---|
| 단위 (도메인) | `TicketTest`, `AssetTest`, `UserTest` | 상태 전이 규칙, SLA 계산, 요청자 취소 권한, 재분류 시 SLA 알림 기록 초기화, 로그인 잠금 규칙 |
| 단위 (보안·정책) | `FileTypePolicyTest`, `TemporaryPasswordGeneratorTest`, `RequestIdFilterTest` | 위조 파일·위험 확장자 거절, 파일명 정리, 임시 비밀번호 규칙(50회 반복), 요청 ID 전파·로그 위조 방지 |
| 단위 (서비스) | `TicketServiceTest`, `TicketTriageServiceTest`, `RuleBasedTriageTest` | Mockito 로 외부 의존성 격리. AI 성공/실패/잘못된 응답 시 fallback |
| 웹 슬라이스 | `TicketControllerTest` (`@WebMvcTest`) | 토큰 없음/위조 401, 일반 사용자의 관리자 API 403, 입력 검증 400, 404/409 에러 포맷 |
| JPA 슬라이스 | `TicketRepositoryTest` (`@DataJpaTest`) | Flyway 로 만든 스키마 위에서 Specification 검색, 이력·처리자 조회, GROUP BY 집계 |
| 통합 | `HelpdeskFlowIntegrationTest` | 로그인 → 자산 배정 → 티켓 접수(자동 분류) → 처리 → 종료 → 대시보드 전체 시나리오, 사용자별 데이터 접근 범위 |
| 통합 (인증) | `AuthIntegrationTest`, `AccountSecurityIntegrationTest` | BCrypt·salt, refresh token 해시, rotation·1회 사용, 비밀번호 변경 전 발급된 토큰 거절, 임시 비밀번호 상태 API 차단, 5회 실패 잠금(실패 횟수가 롤백되지 않는지), 비밀번호 변경 시 다른 기기 세션 폐기, 관리자 초기화·해제 |
| 통합 (협업) | `TicketCollaborationIntegrationTest` | 댓글·내부 메모 가시성, 권한, 종료 티켓 작성 차단, 첨부 업로드→다운로드(한글 파일명)→삭제, 형식·크기·개수 제한 |
| 통합 (알림) | `NotificationIntegrationTest` | 배정·댓글 알림 수신 대상, 내부 메모는 요청자 제외, 남의 알림 접근 차단, SLA 임박/초과 판정·중복 방지·재분류 시 초기화 |

## 테스트와 검증 과정에서 발견해 고친 버그
- **"해결됨 → 종료" 전환 시 해결 시각이 지워짐** (`Ticket#changeStatus`)
- **Jackson 3 에서 `boolean` 필드를 생략하면 400** (`CommentCreateRequest.internal` → `Boolean` 으로 변경)
- **Docker(UTC)에서 접수 시각과 SLA 기한이 9시간 어긋남** (Auditing 에 `Clock` 적용)
- **규칙 기반 분류가 "업무 불가"를 긴급(URGENT)으로 분류** → AI 기준과 같게 한 사람의 업무 불가는 높음(HIGH), 여러 사람의 업무 중단·보안 사고만 긴급 (`RuleBasedTriage`)
- **잘못된 입력으로 서버 오류(500)** — 잘못된 값을 넣어 보내는 테스트로 찾은 것들
  - `?sort=foo` 처럼 없는 필드로 정렬 → 500. 또 `?sort=requester.password` 로 **비밀번호 해시 순서로 정렬**할 수 있었음 → 정렬 가능한 필드를 화이트리스트로 제한하고 그 외는 400 (`SortPolicy`)
  - JSON API 에 `text/plain`, 첨부 API 에 JSON 을 보내면 500 → 415 `C006`. 그 밖의 Spring MVC 4xx 예외도 500 으로 바뀌지 않게 처리 (`GlobalExceptionHandler`)
- **검색어의 `%`, `_` 가 와일드카드로 동작** (`%` 검색 시 전체 목록) → 글자 그대로 검색하도록 이스케이프 (`LikePatterns`)
- **이메일 대소문자 구분** — `Hong@daon.example` 로 가입하면 `hong@...` 으로 로그인 불가, 대소문자만 다른 중복 계정 생성 가능 → 저장·조회 시 소문자로 통일, 기존 데이터는 `V5` 마이그레이션으로 정리
- **리뷰 관점 점검으로 찾은 보안·설계 문제**
  - 임시 비밀번호 강제 변경이 프론트에서만 동작 (API 직접 호출 가능) → 서버에서 `403 AUTH008`
  - 비밀번호 변경·초기화 후에도 기존 access token 이 최대 30분 유효 → 발급 시각과 비밀번호 변경 시각 비교
  - 같은 refresh token 으로 동시에 재발급하면 둘 다 성공 → 삭제된 행 수로 한 번만 허용
  - 일반 사용자가 우선순위를 "긴급"으로 지정해 SLA 를 앞당길 수 있음 → 사용자는 자동 분류만
  - 티켓 접수 시 AI 응답을 기다리는 동안 DB 커넥션 점유 → AI 호출을 트랜잭션 밖으로
- **SNS 로그인 추가 중 발견**
  - SNS 로만 가입한 계정(비밀번호 없음)에 이메일 로그인을 시도하면 null 해시 비교로 500 → 일반 로그인 실패(401)와 같은 처리
  - `SecurityConfig` 가 새 처리기를 주입받자 웹 계층 테스트(`@WebMvcTest`) 12개가 기동 실패 → 테스트 설정에 가짜 빈 추가
  - Google `invalid_client`: 환경변수 client-id 끝에 공백 → 실제 요청 URL 에서 `%20` 을 확인해 원인 특정
  - SNS 에서 이름을 안 주거나(동의 거부) 50자를 넘으면 `users.name` 제약으로 가입이 500 → 이메일 앞부분으로 대체·50자로 자름
  - 같은 SNS 계정으로 동시에 가입하면 UNIQUE 위반 예외가 처리기 밖으로 나가 흰 에러 화면 → 로그인 화면(`AUTH011`)으로



---
[← README](../README.md)
