package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.AssignmentStatus;
import com.ync.lms.domain.Course;
import com.ync.lms.domain.Enrollment;
import com.ync.lms.domain.Submission;
import com.ync.lms.domain.SubmissionStatus;
import com.ync.lms.domain.User;
import com.ync.lms.dto.AssignmentSubmissionStat;
import com.ync.lms.repository.AssignmentRepository;
import com.ync.lms.repository.EnrollmentRepository;
import com.ync.lms.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 통계: 강사 홈, S5 학생 내 통계, I6 강좌 통계
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

    // ===================== 강사 대시보드 (/) =====================

    /** 강사 홈 요약 카드 */
    public record InstructorHomeView(long courseCount, long studentCount, long openAssignmentCount, long ungradedCount) {
    }

    public InstructorHomeView getInstructorHome(Long instructorId) {
        LocalDateTime now = LocalDateTime.now();
        return new InstructorHomeView(
                courseService.findMyCourses(instructorId).size(),
                enrollmentRepository.countByCourseInstructorId(instructorId),
                assignmentRepository.countByCourseInstructorIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        instructorId, now, now),
                submissionRepository.countByAssignmentCourseInstructorIdAndStatus(
                        instructorId, SubmissionStatus.SUBMITTED) // 채점 대기
        );
    }

    // ===================== S5 학생 내 통계 (/) =====================

    private static final long DUE_SOON_HOURS = 72; // 마감 임박 = 3일 이내

    /** 아직 안 낸 진행중 과제 한 줄 */
    public record PendingAssignment(Assignment assignment) {

        private long minutesLeft() {
            return Math.max(0, Duration.between(LocalDateTime.now(), assignment.getEndAt()).toMinutes());
        }

        public boolean isDueSoon() {
            return minutesLeft() <= DUE_SOON_HOURS * 60;
        }

        /** 24시간 이내: "5시간 남음" / "30분 남음", 그 외: 달력 날짜 기준 "D-3" */
        public String remainingText() {
            long minutes = minutesLeft();
            if (minutes < 60) {
                return minutes + "분 남음";
            }
            if (minutes < 24 * 60) {
                return (minutes / 60) + "시간 남음";
            }
            // 남은 시간을 내림하면 10일 뒤가 D-9 로 보이므로, 오늘 날짜와 마감 날짜의 차이로 계산
            long days = ChronoUnit.DAYS.between(LocalDate.now(), assignment.getEndAt().toLocalDate());
            return "D-" + days;
        }

        /** 24시간 이내 빨강, 3일 이내 노랑, 그 외 회색 */
        public String badgeClass() {
            long minutes = minutesLeft();
            if (minutes < 24 * 60) {
                return "badge-danger";
            }
            return isDueSoon() ? "badge-warning" : "badge-secondary";
        }
    }

    /** 채점된 내 과제 한 줄 (차트용) */
    public record ScoredAssignment(String title, int score, int maxScore) {
        public double percent() {
            return score * 100.0 / maxScore;
        }
    }

    /** 학생 대시보드 */
    public record StudentHomeView(long courseCount,
                                  long startedCount,
                                  long submittedCount,
                                  List<PendingAssignment> pending,
                                  List<ScoredAssignment> scored) {

        /** 제출률 (%) = 낸 과제 ÷ 시작된 과제 */
        public int submissionRate() {
            return startedCount == 0 ? 0 : (int) Math.round(submittedCount * 100.0 / startedCount);
        }

        /** 평균 점수 (100점 환산). 채점 0건이면 null */
        public Double averagePercent() {
            return scored.isEmpty() ? null
                    : scored.stream().mapToDouble(ScoredAssignment::percent).average().orElse(0);
        }

        /** 마감 임박 (3일 이내) 개수 */
        public long dueSoonCount() {
            return pending.stream().filter(PendingAssignment::isDueSoon).count();
        }
    }

    /**
     * S5 학생 내 통계: 제출률, 평균 점수, 마감 임박
     */
    public StudentHomeView getStudentHome(Long studentId) {
        List<Long> courseIds = enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(studentId).stream()
                .map(e -> e.getCourse().getId())
                .toList();
        List<Assignment> assignments = courseIds.isEmpty()
                ? List.of()
                : assignmentRepository.findByCourseIdInOrderByEndAtAsc(courseIds); // 마감 빠른 순

        // 과제 id → 내 제출물
        Map<Long, Submission> mySubmissions = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId)
                .stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), Function.identity()));

        List<Assignment> started = assignments.stream()
                .filter(a -> a.getStatus() != AssignmentStatus.UPCOMING)
                .toList();
        long submittedCount = started.stream().filter(a -> mySubmissions.containsKey(a.getId())).count();

        List<PendingAssignment> pending = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.OPEN && !mySubmissions.containsKey(a.getId()))
                .map(PendingAssignment::new)
                .toList();

        List<ScoredAssignment> scored = assignments.stream()
                .map(a -> mySubmissions.get(a.getId()))
                .filter(s -> s != null && s.isGraded())
                .map(s -> new ScoredAssignment(s.getAssignment().getTitle(), s.getScore(), s.getAssignment().getMaxScore()))
                .toList();

        return new StudentHomeView(courseIds.size(), started.size(), submittedCount, pending, scored);
    }

    // ===================== I6 강좌 통계 =====================

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
