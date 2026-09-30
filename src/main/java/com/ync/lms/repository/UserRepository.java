package com.ync.lms.repository;

import com.ync.lms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인: 이메일로 사용자 찾기
    Optional<User> findByEmail(String email);

    // 회원가입 중복 검사
    boolean existsByEmail(String email);

    boolean existsByStudentNo(String studentNo);
}
