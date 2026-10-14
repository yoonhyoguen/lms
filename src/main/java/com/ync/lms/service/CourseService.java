package com.ync.lms.service;

import com.ync.lms.domain.Course;
import com.ync.lms.domain.User;
import com.ync.lms.dto.CourseForm;
import com.ync.lms.exception.BusinessRuleException;
import com.ync.lms.exception.NotFoundException;
import com.ync.lms.repository.AssignmentRepository;
import com.ync.lms.repository.CourseRepository;
import com.ync.lms.repository.EnrollmentRepository;
import com.ync.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 조회 전용, 저장하는 메서드만 따로 @Transactional
public class CourseService {

    // 헷갈리는 문자(0, O, 1, I) 제외한 32글자
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_TRY = 10;

    private final SecureRandom random = new SecureRandom();

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;

    /**
     * I1 강좌 개설: 참여코드를 자동 발급해서 저장한다.
     */
    @Transactional
    public Long create(CourseForm form, Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalStateException("강사 정보를 찾을 수 없습니다."));

        Course course = Course.create(
                form.getTitle(),
                form.getDescription(),
                generateUniqueJoinCode(),
                instructor
        );
        return courseRepository.save(course).getId();
    }

    /**
     * 강사의 "내 강좌" 목록 (최신순)
     */
    public List<Course> findMyCourses(Long instructorId) {
        return courseRepository.findByInstructorIdOrderByCreatedAtDesc(instructorId);
    }

    /**
     * B5: 본인 강좌만 조회 가능.
     * 없는 강좌 → 404, 남의 강좌 → 403
     */
    public Course getMyCourse(Long courseId, Long instructorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("강좌를 찾을 수 없습니다."));
        if (!course.isOwnedBy(instructorId)) {
            throw new AccessDeniedException("본인 강좌가 아닙니다.");
        }
        return course;
    }

    // ===== 추가 기능: 강좌 수정 · 참여코드 재발급 · 삭제 =====

    @Transactional
    public void update(Long courseId, CourseForm form, Long instructorId) {
        Course course = getMyCourse(courseId, instructorId); // B5
        course.update(form.getTitle(), form.getDescription());
    }

    /**
     * 참여코드 재발급. 이미 등록한 학생은 수강 명단(Enrollment)에 있으므로 영향 없음.
     * @return 새 참여코드
     */
    @Transactional
    public String regenerateJoinCode(Long courseId, Long instructorId) {
        Course course = getMyCourse(courseId, instructorId); // B5
        String newCode = generateUniqueJoinCode();            // I1 과 같은 생성 방식 재사용
        course.changeJoinCode(newCode);
        return newCode;
    }

    /**
     * 강좌 삭제: 수강생이나 과제가 하나라도 있으면 불가 (B8 과 같은 방식)
     */
    @Transactional
    public void delete(Long courseId, Long instructorId) {
        Course course = getMyCourse(courseId, instructorId); // B5
        if (enrollmentRepository.existsByCourseId(courseId)) {
            throw new BusinessRuleException("수강생이 있는 강좌는 삭제할 수 없습니다.");
        }
        if (assignmentRepository.existsByCourseId(courseId)) {
            throw new BusinessRuleException("과제가 있는 강좌는 삭제할 수 없습니다.");
        }
        courseRepository.delete(course);
    }

    // ===== 참여코드 생성 =====

    private String generateUniqueJoinCode() {
        for (int i = 0; i < MAX_TRY; i++) {
            String code = randomCode();
            if (!courseRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        // 1조 가지 중 10번 연속 중복 → 사실상 일어나지 않음
        throw new IllegalStateException("참여코드 생성에 실패했습니다. 다시 시도하세요.");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
