package com.ync.lms.config;

import com.ync.lms.domain.User;
import com.ync.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * U4 강사 계정: 회원가입이 없으므로 서버 시작 시 초기 데이터로 생성한다.
 * (개발용 테스트 계정 — 실제 서비스 비밀번호로 쓰지 말 것)
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createInstructorIfNotExists("김강사", "instructor1@lms.com", "instructor1234");
        createInstructorIfNotExists("이강사", "instructor2@lms.com", "instructor1234");
    }

    private void createInstructorIfNotExists(String name, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            return; // 이미 있으면 건너뜀 (MySQL 로 바꿔도 중복 생성 안 되게)
        }
        String encoded = passwordEncoder.encode(rawPassword);
        userRepository.save(User.createInstructor(name, email, encoded));
    }
}
