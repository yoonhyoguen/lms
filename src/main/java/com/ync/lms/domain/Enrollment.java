package com.ync.lms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수강 명단: 어떤 학생이 어떤 강좌를 듣는지 (강좌 N : M 학생 의 중간 테이블)
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_enrollment_course_student",
        columnNames = {"course_id", "student_id"} // 같은 강좌에 같은 학생 2번 등록 금지
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @PrePersist
    void onCreate() {
        this.enrolledAt = LocalDateTime.now();
    }

    public static Enrollment create(Course course, User student) {
        Enrollment enrollment = new Enrollment();
        enrollment.course = course;
        enrollment.student = student;
        return enrollment;
    }
}
