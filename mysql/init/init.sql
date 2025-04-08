-- 데이터베이스 생성 (이미 존재하는 경우 무시)
CREATE DATABASE IF NOT EXISTS tiple;

-- 사용자가 이미 존재하는 경우 삭제
DROP USER IF EXISTS 'tiple'@'%';

-- 모든 호스트에서 접근 가능한 사용자 생성 (native 인증 방식 사용)
CREATE USER 'tiple'@'%' IDENTIFIED WITH mysql_native_password BY '0000';

-- 모든 권한 부여
GRANT ALL PRIVILEGES ON tiple.* TO 'tiple'@'%';
GRANT ALL PRIVILEGES ON *.* TO 'tiple'@'%';

-- 권한 적용
FLUSH PRIVILEGES;