package com.ync.lms.service;

import com.ync.lms.domain.Course;
import com.ync.lms.domain.Enrollment;
import com.ync.lms.domain.User;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.repository.CourseRepository;
import com.ync.lms.repository.EnrollmentRepository;
import com.ync.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * S1 수강 등록: 참여코드로 강좌를 찾아 수강 명단에 추가한다.
     * @return 등록된 강좌 (화면에 "OO 강좌에 등록되었습니다" 표시용)
     */
    @Transactional
    public Course enroll(String joinCode, Long studentId) {
        // 앞뒤 공백 제거 + 대문자로 (소문자로 입력해도 되게)
        String code = joinCode.trim().toUpperCase();

        Course course = courseRepository.findByJoinCode(code)
                .orElseThrow(() -> new FieldValidationException("joinCode", "참여코드가 올바르지 않습니다."));

        if (enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), studentId)) {
            throw new FieldValidationException("joinCode", "이미 수강 중인 강좌입니다.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("학생 정보를 찾을 수 없습니다."));

        enrollmentRepository.save(Enrollment.create(course, student));
        return course;
    }

    /**
     * 학생이 수강 중인 강좌 목록 (최근 등록순)
     */
    public List<Enrollment> findMyEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(studentId);
    }
}
