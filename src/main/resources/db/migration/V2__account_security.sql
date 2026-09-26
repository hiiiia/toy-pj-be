-- =====================================================================
-- V2: 계정 보안 (로그인 시도 제한, 비밀번호 변경/초기화)
-- =====================================================================

-- 연속 로그인 실패 횟수와 잠금 해제 시각
ALTER TABLE users ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP(6);

-- 비밀번호 변경 이력 / 관리자가 초기화한 임시 비밀번호인지 여부
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP(6);
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
