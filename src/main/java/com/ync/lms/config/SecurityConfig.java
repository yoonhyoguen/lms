package com.ync.lms.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1) URL 별 접근 권한 (위에서부터 순서대로 검사)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/signup", "/error/**",
                        "/css/**", "/js/**", "/vendor/**", "/img/**").permitAll()
                .requestMatchers(PathRequest.toH2Console()).permitAll() // 개발용 DB 콘솔
                .requestMatchers("/instructor/**").hasRole("INSTRUCTOR")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
            )

            // 2) 폼 로그인 (로그인 페이지는 8단계에서 .loginPage("/login") 추가)
            .formLogin(form -> form
                .usernameParameter("email")        // 아이디 입력칸 이름 = email
                .defaultSuccessUrl("/", true)      // 로그인 성공 -> 대시보드
                .permitAll()
            )

            // 3) 로그아웃
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
            )

            // 4) H2 콘솔 사용을 위한 예외 (개발용)
            .csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    // 비밀번호 암호화 도구 (회원가입, 강사 초기 데이터, 로그인 비교에 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
