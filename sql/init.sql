-- 0. 기존 DB 삭제 (주의: 데이터 전부 삭제됨)
DROP DATABASE IF EXISTS emergency_system;

-- 1. 데이터베이스 생성 및 사용
CREATE DATABASE IF NOT EXISTS emergency_system;
USE emergency_system;

-- 2. 사용자 정보 테이블 생성
CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL AUTO_INCREMENT,
    user_ID VARCHAR(20) NOT NULL,
    user_name VARCHAR(15) NOT NULL,
    password VARCHAR(40) NOT NULL,
    user_phone VARCHAR(15) NOT NULL,
    birth_date DATE NULL,
    profile_image LONGBLOB DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX username_UNIQUE (user_ID ASC)
);

-- 3. 로그인 가능한 예시 사용자 추가
INSERT INTO users (user_ID, password, user_name, user_phone) VALUES
('test1', '1111', '김철수', '11111111111'),
('test2', '2222', '이민수', '22222222222');

-- 4. 신고 정보 테이블 생성
CREATE TABLE IF NOT EXISTS reports (
    id INT NOT NULL AUTO_INCREMENT,
    report_time TIME DEFAULT NULL,
    address VARCHAR(100) NOT NULL,
    accident_type VARCHAR(50) NOT NULL,
    urgency_level VARCHAR(10) NOT NULL,
    address_summary VARCHAR(100) DEFAULT NULL,
    problem_description VARCHAR(255) DEFAULT NULL,
    false_report TINYINT DEFAULT '0',
    phone_number VARCHAR(20) DEFAULT NULL,
    dialogue_path VARCHAR(255) DEFAULT NULL,
    report_summary TEXT,
    memo TEXT,
    report_date VARCHAR(20) NOT NULL,
    call_duration VARCHAR(20) DEFAULT NULL,
    processing_status VARCHAR(50) NOT NULL DEFAULT '처리중',
    processing_result VARCHAR(255) DEFAULT '처리중',
    reporter VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


