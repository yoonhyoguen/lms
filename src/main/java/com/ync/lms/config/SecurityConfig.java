package com.ync.lms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // H2 콘솔이 켜져 있을 때만 예외 규칙을 넣는다.
    // (꺼져 있으면 PathRequest.toH2Console() 이 필요로 하는 설정이 없어 모든 요청이 500 오류)
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1) URL 별 접근 권한 (위에서부터 순서대로 검사)
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/login", "/signup", "/error/**",
                        "/css/**", "/js/**", "/vendor/**", "/img/**").permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers(PathRequest.toH2Console()).permitAll(); // 개발용 DB 콘솔
                }
                auth.requestMatchers("/instructor/**").hasRole("INSTRUCTOR")
                    .requestMatchers("/student/**").hasRole("STUDENT")
                    .anyRequest().authenticated();
            })

            // 2) 폼 로그인
            .formLogin(form -> form
                .loginPage("/login")               // 우리가 만든 로그인 화면 (GET /login)
                .usernameParameter("email")        // 아이디 입력칸 이름 = email
                .defaultSuccessUrl("/", true)      // 로그인 성공 -> 대시보드
                .permitAll()
            )

            // 3) 로그아웃
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
            );

        // 4) H2 콘솔 사용을 위한 예외 (개발용, 켜져 있을 때만)
        if (h2ConsoleEnabled) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
    }

    // 비밀번호 암호화 도구 (회원가입, 강사 초기 데이터, 로그인 비교에 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
