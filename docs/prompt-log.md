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
