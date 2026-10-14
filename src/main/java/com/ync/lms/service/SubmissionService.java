package com.ync.lms.service;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.Role;
import com.ync.lms.domain.Submission;
import com.ync.lms.domain.User;
import com.ync.lms.dto.SubmissionForm;
import com.ync.lms.exception.BusinessRuleException;
import com.ync.lms.exception.FieldValidationException;
import com.ync.lms.exception.NotFoundException;
import com.ync.lms.repository.SubmissionRepository;
import com.ync.lms.repository.UserRepository;
import com.ync.lms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final FileStorageService fileStorageService;

    /**
     * S3 과제 제출 / 재제출
     * @return true = 재제출(덮어쓰기), false = 첫 제출
     */
    @Transactional
    public boolean submit(Long assignmentId, SubmissionForm form, MultipartFile file, Long studentId) {
        Assignment assignment = assignmentService.getForStudent(assignmentId, studentId); // B2: 수강생만 (403)

        // B1: 시작일시 ≤ 현재 ≤ 종료일시 일 때만
        if (!assignment.isSubmittable()) {
            throw new BusinessRuleException("제출 기간이 아닙니다.");
        }

        Optional<Submission> existing = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);

        // B4: 채점 완료된 제출은 재제출 불가
        if (existing.isPresent() && existing.get().isGraded()) {
            throw new BusinessRuleException("채점 완료된 과제는 다시 제출할 수 없습니다.");
        }

        // 내용 또는 파일 중 하나는 있어야 함 (재제출 시 기존 파일이 있으면 내용 없이도 가능)
        boolean hasContent = StringUtils.hasText(form.getContent());
        boolean hasNewFile = file != null && !file.isEmpty();
        boolean hasOldFile = existing.map(Submission::hasFile).orElse(false);
        if (!hasContent && !hasNewFile && !hasOldFile) {
            throw new FieldValidationException("content", "내용을 입력하거나 파일을 첨부하세요.");
        }

        // 검사를 모두 통과한 뒤에 파일 저장
        FileStorageService.StoredFile stored = fileStorageService.store(file);

        if (existing.isPresent()) {
            // B3: 과제당 1건 → 기존 제출 덮어쓰기
            Submission submission = existing.get();
            submission.resubmit(form.getContent());
            if (stored != null) {
                fileStorageService.delete(submission.changeFile(stored.originalName(), stored.storedName()));
            }
            return true;
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("학생 정보를 찾을 수 없습니다."));
        Submission submission = Submission.create(assignment, student, form.getContent());
        if (stored != null) {
            submission.changeFile(stored.originalName(), stored.storedName());
        }
        submissionRepository.save(submission);
        return false;
    }

    /**
     * 이 과제에 대한 내 제출물 (없으면 empty)
     */
    public Optional<Submission> findMySubmission(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
    }

    /**
     * 제출 첨부 다운로드 권한: 제출한 학생 본인 또는 강좌 주인 강사
     */
    public Submission getDownloadableSubmission(Long submissionId, LoginUser loginUser) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("제출물을 찾을 수 없습니다."));

        boolean allowed = (loginUser.getRole() == Role.INSTRUCTOR)
                ? submission.getAssignment().getCourse().isOwnedBy(loginUser.getId())
                : submission.getStudent().getId().equals(loginUser.getId());

        if (!allowed) {
            throw new AccessDeniedException("이 제출물의 파일을 받을 권한이 없습니다.");
        }
        if (!submission.hasFile()) {
            throw new NotFoundException("첨부파일이 없습니다.");
        }
        return submission;
    }
}
