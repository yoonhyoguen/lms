package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.Course;
import com.ync.lms.domain.Role;
import com.ync.lms.dto.AssignmentForm;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.exception.NotFoundException;
import com.ync.lms.repository.AssignmentRepository;
import com.ync.lms.repository.EnrollmentRepository;
import com.ync.lms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final FileStorageService fileStorageService;

    /**
     * I3 과제 등록 (첨부파일은 선택)
     */
    @Transactional
    public Long create(Long courseId, AssignmentForm form, MultipartFile file, Long instructorId) {
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

        // 검사를 모두 통과한 뒤에 파일 저장
        FileStorageService.StoredFile stored = fileStorageService.store(file);
        if (stored != null) {
            assignment.changeFile(stored.originalName(), stored.storedName());
        }
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
        Assignment assignment = findAssignment(assignmentId);
        if (!assignment.getCourse().isOwnedBy(instructorId)) {
            throw new AccessDeniedException("본인 강좌의 과제가 아닙니다.");
        }
        return assignment;
    }

    /**
     * 첨부파일 다운로드 권한: 강좌 주인 강사(B5) 또는 수강생(B2)
     */
    public Assignment getDownloadableAssignment(Long assignmentId, LoginUser loginUser) {
        Assignment assignment = findAssignment(assignmentId);
        Course course = assignment.getCourse();

        boolean allowed = (loginUser.getRole() == Role.INSTRUCTOR)
                ? course.isOwnedBy(loginUser.getId())
                : enrollmentRepository.existsByCourseIdAndStudentId(course.getId(), loginUser.getId());

        if (!allowed) {
            throw new AccessDeniedException("이 과제의 파일을 받을 권한이 없습니다.");
        }
        if (!assignment.hasFile()) {
            throw new NotFoundException("첨부파일이 없습니다.");
        }
        return assignment;
    }

    /**
     * I3 과제 수정 (새 파일을 고르면 교체, 안 고르면 기존 파일 유지)
     */
    @Transactional
    public void update(Long assignmentId, AssignmentForm form, MultipartFile file, Long instructorId) {
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

        FileStorageService.StoredFile stored = fileStorageService.store(file);
        if (stored != null) {
            String oldStoredName = assignment.changeFile(stored.originalName(), stored.storedName());
            fileStorageService.delete(oldStoredName); // 옛 파일 정리
        }
    }

    /**
     * I3 과제 삭제 (첨부파일도 함께 삭제)
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
        String storedName = assignment.getFileStoredName();
        assignmentRepository.delete(assignment);
        fileStorageService.delete(storedName);
        return courseId;
    }

    private Assignment findAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("과제를 찾을 수 없습니다."));
    }

    // B7: 종료일시는 시작일시보다 뒤
    private void validatePeriod(AssignmentForm form) {
        if (!form.getEndAt().isAfter(form.getStartAt())) {
            throw new FieldValidationException("endAt", "종료일시는 시작일시보다 뒤여야 합니다.");
        }
    }
}
