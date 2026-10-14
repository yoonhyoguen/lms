package com.ync.lms.repository;

import com.ync.lms.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // 강사: 강좌 상세에서 과제 목록 (마감 빠른 순)
    List<Assignment> findByCourseIdOrderByEndAtAsc(Long courseId);

    // S2 학생: 수강 중인 여러 강좌의 과제를 한 번에 (마감 빠른 순)
    List<Assignment> findByCourseIdInOrderByEndAtAsc(List<Long> courseIds);

    // 강사 대시보드: 현재 진행중인 과제 수 (시작일시 ≤ now ≤ 종료일시)
    long countByCourseInstructorIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            Long instructorId, LocalDateTime now1, LocalDateTime now2);
}
