package com.ync.lms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, unique = true, length = 8)
    private String joinCode; // 참여코드 (학생이 입력해서 수강 등록)

    // 강좌 N : 1 강사  →  course 테이블에 instructor_id (FK) 컬럼 생성
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id")
    private User instructor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static Course create(String title, String description, String joinCode, User instructor) {
        Course course = new Course();
        course.title = title;
        course.description = description;
        course.joinCode = joinCode;
        course.instructor = instructor;
        return course;
    }

    // 추가 기능: 강좌 수정 (강사는 바꿀 수 없음)
    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // 추가 기능: 참여코드 재발급
    public void changeJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    // B5: 이 강좌를 개설한 강사인가?
    public boolean isOwnedBy(Long userId) {
        return instructor.getId().equals(userId);
    }
}
