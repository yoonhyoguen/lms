-- =====================================================================
-- 테스트 데이터 (서버가 켜질 때마다 실행됨)
--   * 이미 있는 데이터는 건너뛴다 (WHERE NOT EXISTS)
--     → 재시작해도 중복되지 않고, 바꾼 비밀번호도 덮어쓰지 않는다
--   * 비밀번호는 BCrypt 로 암호화한 값 (원래 값은 README 의 테스트 계정 표 참고)
--   * 날짜는 "처음 실행한 시각" 기준으로 마감 / 진행중 / 예정 과제가 하나씩 생기게 했다
-- =====================================================================

-- ---------- 계정: 강사 2명 (강사는 회원가입이 없으므로 여기서만 만든다) ----------
INSERT INTO users (student_no, name, email, password, role, created_at)
SELECT NULL, '김강사', 'instructor1@lms.com', '$2a$10$wlwA5hKtoI/EBQtknBnwqO30i3sBdFDYVdWbcfxC9U4m8u7vEAaKm', 'INSTRUCTOR', LOCALTIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'instructor1@lms.com');

INSERT INTO users (student_no, name, email, password, role, created_at)
SELECT NULL, '이강사', 'instructor2@lms.com', '$2a$10$wlwA5hKtoI/EBQtknBnwqO30i3sBdFDYVdWbcfxC9U4m8u7vEAaKm', 'INSTRUCTOR', LOCALTIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'instructor2@lms.com');

-- ---------- 계정: 학생 2명 ----------
INSERT INTO users (student_no, name, email, password, role, created_at)
SELECT '2026901', '테스트학생1', 'student1@lms.com', '$2a$10$0POLnbPz9SW9t.mOuvF7iu5yu4sNmXtxrwmdVZ2oKp2hquPf6o9He', 'STUDENT', LOCALTIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student1@lms.com');

INSERT INTO users (student_no, name, email, password, role, created_at)
SELECT '2026902', '테스트학생2', 'student2@lms.com', '$2a$10$0POLnbPz9SW9t.mOuvF7iu5yu4sNmXtxrwmdVZ2oKp2hquPf6o9He', 'STUDENT', LOCALTIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student2@lms.com');

-- ---------- 샘플 강좌 (김강사, 참여코드 SAMPLE26) ----------
INSERT INTO course (title, description, join_code, instructor_id, created_at)
SELECT '샘플 강좌 - 자바 스프링', '테스트 데이터로 만든 강좌입니다.', 'SAMPLE26', u.id, LOCALTIMESTAMP
FROM users u
WHERE u.email = 'instructor1@lms.com'
  AND NOT EXISTS (SELECT 1 FROM course WHERE join_code = 'SAMPLE26');

-- ---------- 수강 등록: 학생 2명 ----------
INSERT INTO enrollment (course_id, student_id, enrolled_at)
SELECT c.id, u.id, LOCALTIMESTAMP
FROM course c, users u
WHERE c.join_code = 'SAMPLE26'
  AND u.email IN ('student1@lms.com', 'student2@lms.com')
  AND NOT EXISTS (SELECT 1 FROM enrollment e WHERE e.course_id = c.id AND e.student_id = u.id);

-- ---------- 과제 3개: 마감 / 진행중 / 예정 ----------
INSERT INTO assignment (course_id, title, content, start_at, end_at, max_score, scores_published, created_at)
SELECT c.id, '샘플 과제 1 (마감)', '이미 마감된 과제입니다.',
       DATEADD('DAY', -14, LOCALTIMESTAMP), DATEADD('DAY', -7, LOCALTIMESTAMP), 100, TRUE, LOCALTIMESTAMP
FROM course c
WHERE c.join_code = 'SAMPLE26'
  AND NOT EXISTS (SELECT 1 FROM assignment a WHERE a.course_id = c.id AND a.title = '샘플 과제 1 (마감)');

INSERT INTO assignment (course_id, title, content, start_at, end_at, max_score, scores_published, created_at)
SELECT c.id, '샘플 과제 2 (진행중)', '지금 제출할 수 있는 과제입니다.',
       DATEADD('DAY', -2, LOCALTIMESTAMP), DATEADD('DAY', 5, LOCALTIMESTAMP), 50, TRUE, LOCALTIMESTAMP
FROM course c
WHERE c.join_code = 'SAMPLE26'
  AND NOT EXISTS (SELECT 1 FROM assignment a WHERE a.course_id = c.id AND a.title = '샘플 과제 2 (진행중)');

INSERT INTO assignment (course_id, title, content, start_at, end_at, max_score, scores_published, created_at)
SELECT c.id, '샘플 과제 3 (예정)', '아직 시작 전인 과제입니다.',
       DATEADD('DAY', 3, LOCALTIMESTAMP), DATEADD('DAY', 10, LOCALTIMESTAMP), 100, TRUE, LOCALTIMESTAMP
FROM course c
WHERE c.join_code = 'SAMPLE26'
  AND NOT EXISTS (SELECT 1 FROM assignment a WHERE a.course_id = c.id AND a.title = '샘플 과제 3 (예정)');

-- ---------- 제출물 ----------
-- 과제 1 (마감): 학생1 제출 + 채점 완료(85점), 학생2 미제출
INSERT INTO submission (assignment_id, student_id, content, status, score, feedback, submitted_at, graded_at)
SELECT a.id, u.id, '과제 1 제출합니다.', 'GRADED', 85, '잘했습니다. 예외 처리를 조금 더 보완하세요.',
       DATEADD('DAY', -8, LOCALTIMESTAMP), DATEADD('DAY', -6, LOCALTIMESTAMP)
FROM assignment a, users u
WHERE a.title = '샘플 과제 1 (마감)' AND u.email = 'student1@lms.com'
  AND NOT EXISTS (SELECT 1 FROM submission s WHERE s.assignment_id = a.id AND s.student_id = u.id);

-- 과제 2 (진행중): 학생1 제출 (채점 대기), 학생2 미제출
INSERT INTO submission (assignment_id, student_id, content, status, score, feedback, submitted_at, graded_at)
SELECT a.id, u.id, '과제 2 제출합니다.', 'SUBMITTED', NULL, NULL, DATEADD('DAY', -1, LOCALTIMESTAMP), NULL
FROM assignment a, users u
WHERE a.title = '샘플 과제 2 (진행중)' AND u.email = 'student1@lms.com'
  AND NOT EXISTS (SELECT 1 FROM submission s WHERE s.assignment_id = a.id AND s.student_id = u.id);
