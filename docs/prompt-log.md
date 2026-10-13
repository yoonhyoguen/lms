# 프롬프트 로그

> 의미 있는 AI 요청과, AI 결과를 검증·수정한 내용을 기록한다.
> 형식: 날짜 / 관련 ID / 요청 / 결과 / 내가 확인·수정한 것

---

## 2026-09-30 · W1 · 프로젝트 생성 (Spring Boot 버전 맞추기)

**요청**
강의 기준(Spring Boot 3.x, Java 17, Security 6)으로 프로젝트를 만들 것.

**문제**
- Spring Initializr 선택지가 4.x뿐이라 4.1.1로 생성된다. → Boot 4는 Security 7 기반이라 강의 코드와 다름.
- 버전 숫자만 3.5로 바꾸면 빌드 실패: Boot 4 전용 의존성 이름(`spring-boot-starter-webmvc`, `spring-boot-h2console`, `*-test` 스타터들)이 3.5에는 없음
- PC에 JDK 21, 25만 있고 17이 없어 toolchain 오류 발생.

**해결**
- `build.gradle`: Boot `3.5.16`으로 변경, 의존성을 3.5 이름으로 교체.
  (`starter-webmvc` → `starter-web`, 테스트 스타터 → `starter-test` + `spring-security-test`)
- `thymeleaf-layout-dialect` 추가 (버전은 Boot가 관리 → 3.4.0)
- `settings.gradle`: foojay 플러그인 추가 → JDK 17 자동 다운로드

**검증**
실행 로그에서 `Spring Boot (v3.5.16)`, `Java 17.0.20.1`, `Tomcat started on port 8080` 확인.

---

## 2026-09-30 · U2 · SecurityConfig 적용 후 임시 계정 로그인 실패

**요청**
SecurityConfig 작성 (URL별 권한, 폼 로그인, BCrypt `PasswordEncoder` 등록).

**문제**
- 적용 후 Spring 기본 임시 계정(`user` / 콘솔에 찍힌 비밀번호)으로 로그인하면 `/login?error`.
- 비밀번호를 정확히 복사해도 실패.

**원인**
- 임시 계정은 비밀번호를 원문으로 저장.
- `BCryptPasswordEncoder`를 Bean으로 등록하면 로그인 시 입력값을 BCrypt 방식으로 비교 → 원문과 절대 일치하지 않음.

**해결**
- 임시 계정은 곧 사라질 계정이므로 고치지 않고, DB 사용자 기반 로그인으로 넘어감.
  - U4: `DataInitializer`에서 강사 계정을 `passwordEncoder.encode()`로 암호화해 저장
  - U2: `CustomUserDetailsService` 등록 → 임시 계정 자동 생성이 꺼짐

**검증**
- 서버 시작 로그에 `Using generated security password` 가 더 이상 나오지 않음.
- 강사 계정 로그인 성공, H2 콘솔에서 비밀번호가 (60자)로 저장된 것 확인.
- 같은 비밀번호인 강사 2명의 암호화 값이 서로 다름 (BCrypt salt).

---

## 2026-09-30 · U1 · 학번 빈칸 제출 시 에러 메시지 2줄 출력

**요청**
회원가입 폼 검증 (`@NotBlank`, `@Pattern`, `@Email`, `@Size`) 과 입력칸별 에러 메시지 표시.

**문제**
- 모든 칸을 비우고 제출하면 학번 칸에만 메시지가 두 줄로 나옴.
  ```
  학번은 숫자만 입력하세요. (최대 20자리)
  학번을 입력하세요.
  ```

**원인**
- 빈 값 `""` 이 `@NotBlank` 에도 걸리고, 정규식 `\d{1,20}` (숫자 1개 이상) 에도 걸림.
- 하나의 값이 두 규칙을 동시에 위반.

**해결**
- 정규식을 `\d{0,20}` 으로 변경 → 빈 값은 `@Pattern` 을 통과하고 `@NotBlank` 만 담당.
- 규칙마다 담당 범위를 나눔: 빈칸 = `@NotBlank`, 형식 = `@Pattern`.

**검증**
| 입력 | 결과 |
|---|---|
| `""` | 학번을 입력하세요. (1줄) |
| `20A1` | 학번은 숫자만 입력하세요. |
| 숫자 21자리 | 학번은 숫자만 입력하세요. |
| `2026001` | 통과 → 가입 완료 |

---

## 2026-09-30 · U3 · 403/404 에러 페이지가 빈 화면으로 보임

**요청**
`templates/error/403.html`, `404.html` 작성 (layout 적용).

**문제**
- curl 로 학생 계정 → `/instructor/courses` 요청 시 상태코드는 403 인데 HTML 내용이 비어 있음.

**원인**
- 응답이 `Content-Type: application/json` 이었음.
- Spring Boot 는 요청의 `Accept` 헤더를 보고 에러 응답 형식을 고른다.
  - 브라우저: `Accept: text/html` → 에러 페이지(HTML)
  - curl / Postman: `Accept: */*` → JSON `{"status":403, ...}`
- 코드 문제가 아니라 테스트 방법의 문제.

**해결**
- 테스트 요청에 `Accept: text/html` 헤더를 추가해 브라우저와 같은 조건으로 재확인.
- 코드 변경 없음.

**검증**
- 학생 → 강사 URL: 403 화면 (제목 "권한 없음 - LMS", 사이드바·학생 메뉴 유지)
- 없는 주소: 404 화면
- 비로그인 → 없는 주소: 로그인 페이지로 이동
- Whitelabel 기본 오류 화면 나오지 않음

**배운 점**
같은 URL 이라도 요청하는 쪽(Accept 헤더)에 따라 응답 형식이 달라진다. Postman 테스트에서 JSON 이 와도 정상.
