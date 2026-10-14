package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.Course;
import com.ync.lms.dto.AssignmentForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseService courseService;

    /**
     * I3 과제 등록
     */
    @Transactional
    public Long create(Long courseId, AssignmentForm form, Long instructorId) {
        Course course = courseService.getMyCourse(courseId, instructorId); // B5: 본인 강좌만
        validatePeriod(form);                                              // B7

        Assignment assignment = Assignment.create(
                course,
                form.getTitle(),
                form.getContent(),
                form.getStartAt(),
                form.getEndAt(),
                form.getMaxScore()
        );
        return assignmentRepository.save(assignment).getId();
    }

    /**
     * 강좌의 과제 목록 (마감 빠른 순)
     */
    public List<Assignment> findByCourse(Long courseId) {
        return assignmentRepository.findByCourseIdOrderByEndAtAsc(courseId);
    }

    // B7: 종료일시는 시작일시보다 뒤
    private void validatePeriod(AssignmentForm form) {
        if (!form.getEndAt().isAfter(form.getStartAt())) {
            throw new FieldValidationException("endAt", "종료일시는 시작일시보다 뒤여야 합니다.");
        }
    }
}
