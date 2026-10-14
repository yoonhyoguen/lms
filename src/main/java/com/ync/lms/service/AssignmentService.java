package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.Course;
import com.ync.lms.dto.AssignmentForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.exception.NotFoundException;
import com.ync.lms.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

    /**
     * B5: 본인 강좌의 과제만. 없으면 404, 남의 강좌 과제면 403
     */
    public Assignment getMyAssignment(Long assignmentId, Long instructorId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("과제를 찾을 수 없습니다."));
        if (!assignment.getCourse().isOwnedBy(instructorId)) {
            throw new AccessDeniedException("본인 강좌의 과제가 아닙니다.");
        }
        return assignment;
    }

    /**
     * I3 과제 수정
     */
    @Transactional
    public void update(Long assignmentId, AssignmentForm form, Long instructorId) {
        Assignment assignment = getMyAssignment(assignmentId, instructorId); // B5
        validatePeriod(form);                                                // B7

        // 값만 바꾸면 트랜잭션이 끝날 때 JPA 가 알아서 UPDATE (save 호출 불필요)
        assignment.update(
                form.getTitle(),
                form.getContent(),
                form.getStartAt(),
                form.getEndAt(),
                form.getMaxScore()
        );
    }

    /**
     * I3 과제 삭제
     * @return 삭제한 과제가 속한 강좌 id (삭제 후 강좌 상세로 돌아가기 위해)
     */
    @Transactional
    public Long delete(Long assignmentId, Long instructorId) {
        Assignment assignment = getMyAssignment(assignmentId, instructorId); // B5

        // TODO [W3] B8: 제출물이 1건이라도 있으면 삭제 불가
        //   if (submissionRepository.existsByAssignmentId(assignmentId)) {
        //       throw new BusinessRuleException("제출물이 있어 삭제할 수 없습니다.");
        //   }

        Long courseId = assignment.getCourse().getId();
        assignmentRepository.delete(assignment);
        return courseId;
    }

    // B7: 종료일시는 시작일시보다 뒤
    private void validatePeriod(AssignmentForm form) {
        if (!form.getEndAt().isAfter(form.getStartAt())) {
            throw new FieldValidationException("endAt", "종료일시는 시작일시보다 뒤여야 합니다.");
        }
    }
}
