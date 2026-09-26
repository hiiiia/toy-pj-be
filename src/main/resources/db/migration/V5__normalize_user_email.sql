-- 이메일을 대소문자 구분 없이 다루도록 바꾸면서(User.normalizeEmail), 기존 데이터도 같은 형태로 맞춘다.
-- 대소문자만 다른 중복 계정이 이미 있다면 uk_users_email 위반으로 이 마이그레이션이 실패한다.
-- 그 경우 어느 계정을 남길지 사람이 정리한 뒤 다시 실행해야 한다(자동으로 합치면 데이터가 사라질 수 있음).
UPDATE users
SET email = LOWER(TRIM(email))
WHERE email <> LOWER(TRIM(email));
