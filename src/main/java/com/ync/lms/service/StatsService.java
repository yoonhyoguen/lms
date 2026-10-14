package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.AssignmentStatus;
import com.ync.lms.domain.Course;
import com.ync.lms.domain.Enrollment;
import com.ync.lms.domain.User;
import com.ync.lms.dto.AssignmentSubmissionStat;
import com.ync.lms.repository.AssignmentRepository;
import com.ync.lms.repository.EnrollmentRepository;
import com.ync.lms.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 통계 (I6 강사 대시보드)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    /** 과제 한 개의 통계 */
    public record AssignmentStatRow(Assignment assignment,
                                    long studentCount,
                                    long submittedCount,
                                    long gradedCount,
                                    Double averageScore,
                                    List<User> notSubmittedStudents) {

        /** 제출률 (%) */
        public int submissionRate() {
            return studentCount == 0 ? 0 : (int) Math.round(submittedCount * 100.0 / studentCount);
        }

        /** 평균 점수를 100점 만점으로 환산 (채점 0건이면 null) */
        public Double averagePercent() {
            return averageScore == null ? null : averageScore * 100.0 / assignment.getMaxScore();
        }

        /** 예정 과제는 아직 제출할 수 없으므로 제출률·미제출 계산에서 뺀다 */
        public boolean isStarted() {
            return assignment.getStatus() != AssignmentStatus.UPCOMING;
        }
    }

    /** 강좌 전체 통계 */
    public record CourseStatsView(Course course, long studentCount, List<AssignmentStatRow> rows) {

        public long assignmentCount() {
            return rows.size();
        }

        /** 전체 제출률 (%) = 시작된 과제들의 제출 수 합 ÷ (수강생 수 × 시작된 과제 수) */
        public int overallSubmissionRate() {
            List<AssignmentStatRow> started = rows.stream().filter(AssignmentStatRow::isStarted).toList();
            long expected = studentCount * started.size();
            long submitted = started.stream().mapToLong(AssignmentStatRow::submittedCount).sum();
            return expected == 0 ? 0 : (int) Math.round(submitted * 100.0 / expected);
        }

        /** 전체 평균 점수 (100점 환산, 채점 건수로 가중평균). 채점 0건이면 null */
        public Double overallAveragePercent() {
            long graded = rows.stream().mapToLong(AssignmentStatRow::gradedCount).sum();
            if (graded == 0) {
                return null;
            }
            double weightedSum = rows.stream()
                    .filter(r -> r.averagePercent() != null)
                    .mapToDouble(r -> r.averagePercent() * r.gradedCount())
                    .sum();
            return weightedSum / graded;
        }

        /** 미제출이 하나라도 있는 (시작된) 과제 */
        public List<AssignmentStatRow> rowsWithNotSubmitted() {
            return rows.stream()
                    .filter(r -> r.isStarted() && !r.notSubmittedStudents().isEmpty())
                    .toList();
        }
    }

    /**
     * I6 강좌 통계 (B5: 본인 강좌만)
     */
    public CourseStatsView getCourseStats(Long courseId, Long instructorId) {
        Course course = courseService.getMyCourse(courseId, instructorId); // B5

        List<User> students = enrollmentRepository.findByCourseIdOrderByEnrolledAtAsc(courseId).stream()
                .map(Enrollment::getStudent)
                .sorted(Comparator.comparing(User::getStudentNo))
                .toList();

        // group by 결과: 과제 id → 통계
        Map<Long, AssignmentSubmissionStat> statByAssignment = submissionRepository.countByAssignmentInCourse(courseId)
                .stream()
                .collect(Collectors.toMap(AssignmentSubmissionStat::assignmentId, Function.identity()));

        // 과제 id → 제출한 학생 id 들 (미제출자 계산용)
        Map<Long, Set<Long>> submittedStudentIds = submissionRepository.findByAssignmentCourseId(courseId).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getAssignment().getId(),
                        Collectors.mapping(s -> s.getStudent().getId(), Collectors.toSet())));

        List<AssignmentStatRow> rows = assignmentRepository.findByCourseIdOrderByEndAtAsc(courseId).stream()
                .map(assignment -> {
                    AssignmentSubmissionStat stat = statByAssignment.get(assignment.getId());
                    Set<Long> submitted = submittedStudentIds.getOrDefault(assignment.getId(), Set.of());
                    List<User> notSubmitted = students.stream()
                            .filter(student -> !submitted.contains(student.getId()))
                            .toList();
                    return new AssignmentStatRow(
                            assignment,
                            students.size(),
                            stat == null ? 0 : stat.submittedCount(),
                            stat == null ? 0 : stat.gradedCount(),
                            stat == null ? null : stat.averageScore(),
                            notSubmitted);
                })
                .toList();

        return new CourseStatsView(course, students.size(), rows);
    }
}
