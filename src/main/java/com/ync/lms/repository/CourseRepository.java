package com.ync.lms.repository;

import com.ync.lms.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 강사: 내가 개설한 강좌 목록 (최신순)
    List<Course> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);

    // 학생: 참여코드로 강좌 찾기 (S1)
    Optional<Course> findByJoinCode(String joinCode);

    // 참여코드 발급 시 중복 확인 (I1)
    boolean existsByJoinCode(String joinCode);
}
