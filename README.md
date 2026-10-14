# LMS — 과제 제출·채점 관리 시스템

강사는 강좌를 개설하고 과제를 내고 채점하며, 학생은 참여코드로 수강 등록 후 기간 안에 과제를 제출하고 결과를 확인하는 **학습 관리 시스템(LMS)** 입니다.
REST API 대신 **서버 렌더링(Controller → Model → Thymeleaf)** 으로 만들었습니다.

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| 백엔드 | Spring Boot 3.5 · Java 17 · Spring Data JPA |
| DB | H2 (메모리, 개발용) |
| 인증 | Spring Security 6 · 세션 기반 폼 로그인 · BCrypt |
| 화면 | Thymeleaf · Layout Dialect · thymeleaf-extras-springsecurity6 |
| UI 템플릿 | SB Admin 2 (Bootstrap 4.6 · jQuery · Font Awesome 5) |
| 차트·표 | Chart.js 2.9 · DataTables |
| 테스트 | JUnit 5 · AssertJ · `@DataJpaTest` |

---

## 실행 방법

**필요한 것:** JDK 17 이상 아무 버전 (Gradle 실행용). 프로젝트가 쓰는 JDK 17 이 없으면 Gradle 이 자동으로 내려받습니다.

```bash
./gradlew bootRun
```
또는 IntelliJ 에서 `LmsApplication` 실행 → 브라우저에서 **http://localhost:8080**

- DB 는 H2 **메모리** 모드라서 서버를 재시작하면 데이터가 초기화됩니다. (강사 계정은 자동 생성)
- 업로드 파일은 프로젝트 폴더의 `uploads/` 에 저장됩니다. (Git 제외)
- DB 확인: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:lmsdb`, User `sa`)

### 테스트 계정

| 역할 | 이메일 | 비밀번호 |
|---|---|---|
| 강사 | `instructor1@lms.com` (김강사) | [`DataInitializer.java`](src/main/java/com/ync/lms/config/DataInitializer.java) 참고 |
| 강사 | `instructor2@lms.com` (이강사) | 〃 (본인 강좌 검증 B5 시험용) |
| 학생 | 직접 회원가입 | |

> 강사 계정은 회원가입이 불가능하고 서버 시작 시 초기 데이터로 만들어집니다. (U4)
> 개발용 테스트 계정입니다.

---

## 요구사항 구현

### 공통
| ID | 기능 | 구현 |
|---|---|---|
| U1 | 학생 회원가입 | 학번·이름·이메일·비밀번호, 형식 검증 + 이메일/학번 중복 검사, BCrypt 암호화 |
| U2 | 로그인·로그아웃 | Spring Security 폼 로그인 (이메일), 로그아웃은 POST (CSRF) |
| U3 | 역할별 메뉴 | `sec:authorize` 로 강사/학생 사이드바 분리, URL 권한은 SecurityConfig |
| U4 | 강사 계정 | `DataInitializer` 초기 데이터 (가입 불가) |

### 강사
| ID | 기능 | 구현 |
|---|---|---|
| I1 | 강좌 개설 | 참여코드 8자리 자동 발급 (`SecureRandom`, 헷갈리는 `0 O 1 I` 제외, 중복 확인) |
| I2 | 수강생 목록 | 강좌 상세에서 학번·이름·이메일·등록일 |
| I3 | 과제 등록·수정·삭제 | 시작/종료일시 · 배점 · 첨부파일 1개 (UUID 저장, 10MB 제한) |
| I4 | 제출 현황 | 과제별 제출/미제출 표 + 요약 카드, DataTables 검색·정렬 |
| I5 | 채점·피드백 | 점수 + 코멘트, 재채점 가능 |
| I6 | 통계 대시보드 | 강좌별 수강생·과제·제출률·평균 카드, Chart.js 차트 2개, 과제별 미제출자 |

### 학생
| ID | 기능 | 구현 |
|---|---|---|
| S1 | 수강 등록 | 참여코드 입력 (대소문자·앞뒤 공백 무시) |
| S2 | 내 과제 목록 | 예정/진행중/마감 배지 (저장하지 않고 시간으로 계산) |
| S3 | 과제 제출·재제출 | 기간 안에서만, 내용 + 파일 첨부, 재제출은 덮어쓰기 |
| S4 | 결과 확인 | 점수 / 배점, 피드백 |
| S5 | 내 통계 | 제출률, 평균 점수(100점 환산), 마감 임박(3일 이내) 목록, 점수 차트 |

---

## 핵심 비즈니스 규칙

> 화면에서 막는 것은 "편의", 서버에서 막는 것은 "보안".
> **모든 규칙은 Service 에서 검사**하고, 필요한 곳은 화면에서도 미리 막았습니다.

| # | 규칙 | 서버 검사 위치 | 위반 시 |
|---|---|---|---|
| B1 | 제출은 시작일시 ≤ 현재 ≤ 종료일시 일 때만 | `Assignment.isSubmittable()` → `SubmissionService.submit` | "제출 기간이 아닙니다" 알림 (화면은 입력칸 비활성화) |
| B2 | 수강 등록된 학생만 과제 열람·제출 | `AssignmentService.getForStudent` | 403 |
| B3 | 과제당 학생 1건, 재제출은 덮어쓰기 | DB unique (assignment, student) + `Submission.resubmit()` | 기존 제출 수정 |
| B4 | 채점 완료(GRADED) 제출은 재제출 불가 | `SubmissionService.submit` | "채점 완료된 과제" 알림 |
| B5 | 강사는 본인 강좌의 과제만 수정·삭제·채점 | `Course.isOwnedBy()` → `getMyCourse` / `getMyAssignment` / `getForGrading` | 403 |
| B6 | 점수는 0 이상 배점 이하 | `SubmissionService.grade` (배점이 과제마다 달라 Service 에서) | 폼 에러 |
| B7 | 종료일시는 시작일시보다 뒤 | `AssignmentService.validatePeriod` | 폼 에러 |
| B8 | 제출이 1건이라도 있는 과제는 삭제 불가 | `AssignmentService.delete` | "제출물이 있어 삭제 불가" 알림 |

오류 표시 방식은 두 가지 예외로 나눴습니다.
- `FieldValidationException` → 해당 **입력칸 아래** 에러 (B6, B7, 회원가입 중복 등)
- `BusinessRuleException` → 화면 위 **알림** (B1, B4, B8)

---

## 화면 / URL

| 역할 | URL | 화면 |
|---|---|---|
| 공통 | `/login` · `/signup` | 로그인 · 학생 회원가입 |
| 공통 | `/` | 역할별 대시보드 (강사: 요약 카드 / 학생: S5 내 통계) |
| 강사 | `/instructor/courses` · `/new` · `/{id}` | 내 강좌 · 개설 · 상세(참여코드, 과제, 수강생) |
| 강사 | `/instructor/assignments/new` · `/{id}/edit` | 과제 등록 · 수정/삭제 |
| 강사 | `/instructor/assignments/{id}/submissions` | 제출 현황 + 미제출자 |
| 강사 | `/instructor/submissions/{id}/grade` | 채점 · 피드백 |
| 강사 | `/instructor/stats` | 통계 대시보드 |
| 학생 | `/student/courses/join` | 참여코드 입력 + 수강 중인 강좌 |
| 학생 | `/student/assignments` · `/{id}` | 내 과제 목록 · 상세 + 제출 |
| 학생 | `/student/submissions` | 내 제출 · 점수 · 피드백 |
| 공통 | `/assignments/{id}/file` · `/submissions/{id}/file` | 첨부 다운로드 (권한 검사) |
| 공통 | `error/403` · `error/404` · `error` | 권한 없음 · 없는 페이지 · 그 외 오류 |

---

## 설계

- **ERD:** [`docs/erd.md`](docs/erd.md) — USERS · COURSE · ENROLLMENT · ASSIGNMENT · SUBMISSION
- **저장하지 않고 계산하는 값:** 과제 배지, 미제출자, 제출률, 평균 점수
- 파일은 디스크에, DB 에는 원래 이름 + 저장 이름(UUID) 만 저장

### 프로젝트 구조

```
src/main/java/com/ync/lms
├─ config/        SecurityConfig, DataInitializer
├─ security/      LoginUser, CustomUserDetailsService
├─ domain/        User, Course, Enrollment, Assignment, Submission (+ 상태 enum)
├─ repository/    Spring Data JPA (I6 통계는 group by @Query)
├─ service/       비즈니스 규칙 B1~B8, 파일 저장, 통계
├─ dto/           폼 입력 객체 (Entity 를 폼에 직접 쓰지 않음)
├─ exception/     FieldValidationException, BusinessRuleException, NotFoundException
└─ controller/    강사/학생/공통 컨트롤러, 공통 예외 처리

src/main/resources
├─ static/        SB Admin 2 (필요한 파일만)
└─ templates/     layout/ · fragments/ · auth/ · instructor/ · student/ · error/
```

---

## 테스트

```bash
./gradlew test
```

| 테스트 | 확인하는 것 |
|---|---|
| `FileStorageServiceTest` (7) | UUID 저장, 같은 이름 덮어쓰기 방지, 경로 조작(`../`) 차단, 삭제 |
| `SubmissionRepositoryTest` (2) | I6 group by 쿼리: 제출 수, 채점 수, 평균(채점 전 null 제외), 다른 강좌 미포함 |
| `LmsApplicationTests` (1) | 애플리케이션 시작 |

이 외에 요구사항 ID 와 B1~B8 을 처음부터 끝까지 확인하는 **통합 점검 45개 항목**을 실제 서버에 요청을 보내는 방식으로 수행했습니다. (강사2의 남의 강좌 접근, 개발자도구로 막힌 버튼을 우회한 강제 제출 등 포함)

---

## AI 활용

Claude 를 사용해 요구사항 ID 단위로 작게 요청하고, 생성된 코드를 실행·테스트해 확인한 뒤 커밋했습니다.
AI 가 만든 코드에서 발견해 고친 문제와 검증 과정은 [`docs/prompt-log.md`](docs/prompt-log.md) 에 기록했습니다.

---

## 알려진 제한

- H2 메모리 DB: 재시작하면 데이터가 사라짐 (MySQL 전환은 선택 사항)
- 첨부파일은 과제·제출물마다 1개
- 강좌 수정/삭제, 수강 취소, 비밀번호 변경 기능 없음
