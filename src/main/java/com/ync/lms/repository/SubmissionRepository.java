package com.ync.lms.repository;

import com.ync.lms.domain.Submission;
import com.ync.lms.domain.SubmissionStatus;
import com.ync.lms.dto.AssignmentSubmissionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 학생 대시보드 "최근 피드백": 최근 채점 + 점수 공개된 제출물 3개
    List<Submission> findTop3ByStudentIdAndStatusAndAssignmentScoresPublishedTrueOrderByGradedAtDesc(
            Long studentId, SubmissionStatus status);

    // 강사 대시보드: 내 강좌들의 채점 대기(SUBMITTED) 수
    long countByAssignmentCourseInstructorIdAndStatus(Long instructorId, SubmissionStatus status);

    // I6 통계: 강좌의 모든 제출물 (과제별 미제출자 계산용)
    List<Submission> findByAssignmentCourseId(Long courseId);

    // I6 통계: 강좌의 과제별 제출 수, 채점 수, 평균·최고·최저 점수 (group by)
    //   count(s.score) / avg / max / min 은 null(채점 전)을 빼고 계산한다
    @Query("""
            select new com.ync.lms.dto.AssignmentSubmissionStat(
                       s.assignment.id, count(s), count(s.score), avg(s.score), max(s.score), min(s.score))
            from Submission s
            where s.assignment.course.id = :courseId
            group by s.assignment.id
            """)
    List<AssignmentSubmissionStat> countByAssignmentInCourse(@Param("courseId") Long courseId);
}
