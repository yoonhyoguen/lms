package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.AssignmentStatus;
import com.ync.lms.domain.Course;
import com.ync.lms.domain.Submission;
import com.ync.lms.exception.NotFoundException;
import com.ync.lms.repository.AssignmentRepository;
import com.ync.lms.repository.CourseRepository;
import com.ync.lms.repository.EnrollmentRepository;
import com.ync.lms.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 추가 기능: 학생 강좌 홈 (이 강좌의 과제 + 내 성적표). 학교 LMS 강좌 홈 + 성적 화면 참고
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    /** 과제 한 줄: 과제 + 내 제출물 (안 냈으면 null) */
    public record CourseHomeRow(Assignment assignment, Submission submission) {

        public boolean isSubmitted() {
            return submission != null;
        }

        public boolean isGraded() {
            return submission != null && submission.isGraded();
        }

        public boolean isStarted() {
            return assignment.getStatus() != AssignmentStatus.UPCOMING;
        }

        /** 내 제출 상태 글자: 예정 / 미제출 / 제출됨 / 채점 완료 */
        public String myStatusLabel() {
            if (submission != null) {
                return submission.getStatus().getLabel();
            }
            return isStarted() ? "미제출" : "예정";
        }

        public String myStatusBadge() {
            if (submission != null) {
                return submission.getStatus().getBadgeClass();
            }
            return isStarted() ? "badge-danger" : "badge-light";
        }
    }

    /** 강좌 홈 전체 */
    public record CourseHomeView(Course course, List<CourseHomeRow> rows, boolean gradedOnly) {

        /** 합계에 들어가는 과제: 체크 시 채점된 것만, 해제 시 시작된 과제 전부(채점 안 된 것은 0점) */
        private List<CourseHomeRow> counted() {
            return rows.stream()
                    .filter(r -> gradedOnly ? r.isGraded() : r.isStarted())
                    .toList();
        }

        public int totalScore() {
            return counted().stream().filter(CourseHomeRow::isGraded).mapToInt(r -> r.submission().getScore()).sum();
        }

        public int totalMax() {
            return counted().stream().mapToInt(r -> r.assignment().getMaxScore()).sum();
        }

        /** 백분율 (계산할 과제가 없으면 null) */
        public Double percent() {
            return totalMax() == 0 ? null : totalScore() * 100.0 / totalMax();
        }
    }

    /**
     * B2: 수강 등록된 학생만. 없는 강좌 → 404, 수강생이 아니면 → 403
     */
    public CourseHomeView getCourseHome(Long courseId, Long studentId, boolean gradedOnly) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("강좌를 찾을 수 없습니다."));
        if (!enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new AccessDeniedException("수강 중인 강좌가 아닙니다.");
        }

        // 과제 id → 내 제출물 (이 강좌 것만)
        Map<Long, Submission> mySubmissions = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .filter(s -> s.getAssignment().getCourse().getId().equals(courseId))
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), Function.identity()));

        List<CourseHomeRow> rows = assignmentRepository.findByCourseIdOrderByEndAtAsc(courseId).stream()
                .map(a -> new CourseHomeRow(a, mySubmissions.get(a.getId())))
                .toList();

        return new CourseHomeView(course, rows, gradedOnly);
    }
}
