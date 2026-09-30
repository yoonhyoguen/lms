package com.ync.lms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users") // user 는 DB 예약어라 users 로
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 용 기본 생성자 (외부에서 new User() 금지)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 1, 2, 3... 자동 증가
    private Long id;

    @Column(unique = true, length = 20)
    private String studentNo; // 학번 (강사는 null)

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email; // 로그인 아이디

    @Column(nullable = false)
    private String password; // BCrypt 로 암호화된 값만 저장

    @Enumerated(EnumType.STRING) // DB에 "STUDENT" 문자열로 저장
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist // 처음 저장되기 직전에 자동 실행
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== 생성 메서드: 역할별로 필요한 값만 받도록 =====

    public static User createStudent(String studentNo, String name, String email, String encodedPassword) {
        User user = new User();
        user.studentNo = studentNo;
        user.name = name;
        user.email = email;
        user.password = encodedPassword;
        user.role = Role.STUDENT;
        return user;
    }

    public static User createInstructor(String name, String email, String encodedPassword) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.password = encodedPassword;
        user.role = Role.INSTRUCTOR;
        return user;
    }
}
