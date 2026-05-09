-- ============================================================
-- Smart CE Portal – Reference Schema
-- (Hibernate auto-creates tables from entities; this is
--  provided for reference / manual setup only)
-- Run against: smart_ce_portal database
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_ce_portal
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_ce_portal;

-- Users (students, teachers, admin)
CREATE TABLE IF NOT EXISTS users (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(100) NOT NULL UNIQUE,   -- PRN / Emp-ID
    password  VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email     VARCHAR(150) NOT NULL,
    role      ENUM('STUDENT','TEACHER','ADMIN') NOT NULL,
    enabled   TINYINT(1)   NOT NULL DEFAULT 1
);

-- Exam Tests
CREATE TABLE IF NOT EXISTS exam_tests (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                VARCHAR(200) NOT NULL,
    subject              VARCHAR(100) NOT NULL,
    duration_minutes     INT          NOT NULL DEFAULT 60,
    total_marks          INT          NOT NULL DEFAULT 50,
    negative_marking     DOUBLE       NOT NULL DEFAULT 0.25,
    scheduled_date_time  DATETIME     NOT NULL,
    status               ENUM('SCHEDULED','PUBLISHED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    created_by           BIGINT,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Questions
CREATE TABLE IF NOT EXISTS questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id         BIGINT       NOT NULL,
    question_text   TEXT         NOT NULL,
    option_a        VARCHAR(500) NOT NULL,
    option_b        VARCHAR(500) NOT NULL,
    option_c        VARCHAR(500) NOT NULL,
    option_d        VARCHAR(500) NOT NULL,
    correct_option  ENUM('A','B','C','D') NOT NULL,
    marks           INT NOT NULL DEFAULT 1,
    FOREIGN KEY (test_id) REFERENCES exam_tests(id) ON DELETE CASCADE
);

-- Test Attempts (one per student per test)
CREATE TABLE IF NOT EXISTS test_attempts (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id         BIGINT         NOT NULL,
    test_id            BIGINT         NOT NULL,
    start_time         DATETIME,
    end_time           DATETIME,
    score              DOUBLE         NOT NULL DEFAULT 0,
    correct_count      INT            NOT NULL DEFAULT 0,
    wrong_count        INT            NOT NULL DEFAULT 0,
    unattempted_count  INT            NOT NULL DEFAULT 0,
    submitted          TINYINT(1)     NOT NULL DEFAULT 0,
    tab_switch_count   INT            NOT NULL DEFAULT 0,
    auto_submitted     TINYINT(1)     NOT NULL DEFAULT 0,
    FOREIGN KEY (student_id) REFERENCES users(id)       ON DELETE CASCADE,
    FOREIGN KEY (test_id)    REFERENCES exam_tests(id)  ON DELETE CASCADE,
    UNIQUE KEY uq_student_test (student_id, test_id)
);

-- Student Answers
CREATE TABLE IF NOT EXISTS student_answers (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id       BIGINT NOT NULL,
    question_id      BIGINT NOT NULL,
    selected_option  ENUM('A','B','C','D'),     -- NULL = unattempted
    correct          TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (attempt_id)  REFERENCES test_attempts(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id)     ON DELETE CASCADE
);

-- Default admin (password: admin123 bcrypt-hashed – handled by DataInitializer on startup)
-- No manual insert needed; DataInitializer.java seeds admin on first run.
