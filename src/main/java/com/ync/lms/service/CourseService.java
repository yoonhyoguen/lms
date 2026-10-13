package com.ync.lms.service;

import com.ync.lms.domain.Course;
import com.ync.lms.domain.User;
import com.ync.lms.dto.CourseForm;
import com.ync.lms.repository.CourseRepository;
import com.ync.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
