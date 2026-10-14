package com.ync.lms.repository;

import com.ync.lms.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // 강사: 강좌 상세에서 과제 목록 (마감 빠른 순)
    List<Assignment> findByCourseIdOrderByEndAtAsc(Long courseId);

    // S2 학생: 수강 중인 여러 강좌의 과제를 한 번에 (마감 빠른 순)
    List<Assignment> findByCourseIdInOrderByEndAtAsc(List<Long> courseIds);
}
