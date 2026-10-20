# 프로젝트 맥락 (AI 에게 요청할 때 첫 메시지에 붙여 넣는 내용)

## 1. 무엇을 만드나
강사가 과제를 내고, 수강생이 기간 안에 제출하고, 강사가 채점하고, 결과를 통계로 보는 LMS.
REST API 가 아니라 **서버 렌더링** (Controller → Model → Thymeleaf).

## 2. 버전 (이 버전에 맞는 코드만)
| 구분 | 버전 |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.x (Boot 4 문법 쓰지 말 것) |
| Spring Security | 6.x — `SecurityFilterChain` 빈 방식, 세션 폼 로그인, BCrypt |
| JPA | Spring Data JPA, Hibernate 6 |
| DB | H2 파일 모드 (`jdbc:h2:file:./data/lmsdb`), `ddl-auto: update`, 테스트 데이터 `data.sql` |
| 화면 | Thymeleaf + Layout Dialect + extras-springsecurity6 |
| UI | SB Admin 2 (Bootstrap **4.6**, jQuery), Chart.js **2.9** (`yAxes` 문법), DataTables |

## 3. 패키지 구조 (`com.ync.lms`)
```
config/      SecurityConfig
security/    LoginUser (UserDetails), CustomUserDetailsService
domain/      User, Course, Enrollment, Assignment, Submission + enum (Role, AssignmentStatus, SubmissionStatus)
repository/  Spring Data JPA (통계는 JPQL group by + DTO 생성자)
service/     비즈니스 규칙 검사는 전부 여기
dto/         폼 입력 객체 (Entity 를 폼에 직접 바인딩하지 않음)
exception/   FieldValidationException (입력칸 에러) · BusinessRuleException (알림) · NotFoundException (404)
controller/  /instructor/** · /student/** · 공통
```
templates: `layout/layout.html` + `fragments/`(sidebar · topbar · footer), `auth/` `instructor/` `student/` `error/`

## 4. 지켜야 할 규칙
- **검증은 Service 에서.** 화면 버튼 비활성화는 편의일 뿐, 개발자도구로 강제 요청해도 서버가 막아야 한다.
- **권한은 2겹.** URL 역할 검사(SecurityConfig) + 본인 소유 검사(Service: `getMyCourse`, `getMyAssignment`, `getForStudent`, `getForGrading`)
- 과제 상태(예정/진행중/마감)는 **DB 에 저장하지 않고** 현재 시각으로 계산 (`Assignment.getStatus()`)
- 링크·리소스 경로는 `th:href="@{/...}"` (하위 URL 에서 CSS 깨짐 방지)
- 폼은 POST + CSRF 토큰 (Thymeleaf `th:action` 이 자동으로 넣음), 로그아웃도 POST
- JS 안에 `[[ ]]` 배열을 쓰는 `<script>` 는 `th:inline="none"` (Thymeleaf 인라인 식과 충돌)
- 예외로 트랜잭션이 롤백되면 엔티티가 분리되므로, 폼을 다시 보여줄 때는 **다시 조회**해서 쓴다
- 커밋 메시지에 요구사항 ID 포함 (예: `feat: S3 제출 기간 검증`)

## 5. 비즈니스 규칙 요약
| # | 규칙 |
|---|---|
| B1 | 제출은 `startAt ≤ 현재 ≤ endAt` 일 때만 |
| B2 | 수강 등록된 학생만 과제 열람·제출 (아니면 403) |
| B3 | 과제당 학생 1건, 기간 내 재제출은 덮어쓰기 |
| B4 | 채점 완료(GRADED)된 제출은 재제출 불가 |
| B5 | 강사는 본인 강좌의 과제만 수정·삭제·채점 (아니면 403) |
| B6 | 점수는 0 이상 배점 이하 |
| B7 | 종료일시 > 시작일시 |
| B8 | 제출이 1건이라도 있는 과제는 삭제 불가 |

## 6. 화면 맵
| 역할 | 메뉴 | URL |
|---|---|---|
| 공통 | 로그인 · 회원가입(학생) | `/login` · `/signup` |
| 공통 | 대시보드 (역할별) · 캘린더 · 내 정보 | `/` · `/calendar` · `/me` |
| 강사 | 내 강좌 → 강좌 상세 (참여코드 · 과제 · 수강생) | `/instructor/courses` → `/instructor/courses/{id}` |
| 강사 | 과제 등록 · 수정 | `/instructor/assignments/new` · `/{id}/edit` |
| 강사 | 제출 현황 → 채점 | `/instructor/assignments/{id}/submissions` → `/instructor/submissions/{id}/grade` |
| 강사 | 통계 | `/instructor/stats?courseId=` |
| 학생 | 수강 등록 → 강좌 홈 | `/student/courses/join` → `/student/courses/{id}` |
| 학생 | 내 과제 → 상세 + 제출 | `/student/assignments` → `/student/assignments/{id}` |
| 학생 | 내 제출 · 점수 · 피드백 | `/student/submissions` |

## 7. 요청 형식
```
[요청] 요구사항 ID + 한 가지 기능 (예: S3 학생의 과제 제출 SubmissionService.submit())
[제약] 위 규칙 중 해당되는 것, 만들지 말 것
[출력] 코드 + 검사 로직이 왜 필요한지 짧은 설명
```
