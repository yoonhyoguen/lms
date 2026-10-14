package com.ync.lms.repository;

import com.ync.lms.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // B3 / S3: 이 학생이 이 과제에 이미 제출했나? (있으면 덮어쓰기)
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // B8: 제출물이 1건이라도 있나?
    boolean existsByAssignmentId(Long assignmentId);

    // I4 강사: 과제별 제출 목록
    List<Submission> findByAssignmentId(Long assignmentId);

    // S4 학생: 내 제출 목록 (최근 제출순)
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
}
