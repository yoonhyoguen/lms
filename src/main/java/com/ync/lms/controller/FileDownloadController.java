package com.ync.lms.controller;

import com.ync.lms.domain.Assignment;
import com.ync.lms.domain.Submission;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.AssignmentService;
import com.ync.lms.service.FileStorageService;
import com.ync.lms.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

/**
 * 첨부파일 다운로드 (과제 첨부, 제출 첨부). 강사·학생 공통 주소, 권한은 Service 에서 검사.
 */
@Controller
@RequiredArgsConstructor
public class FileDownloadController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final FileStorageService fileStorageService;

    // 과제 첨부: 강좌 주인 강사 또는 수강생
    @GetMapping("/assignments/{assignmentId}/file")
    public ResponseEntity<Resource> downloadAssignmentFile(@AuthenticationPrincipal LoginUser loginUser,
                                                           @PathVariable Long assignmentId) {
        Assignment assignment = assignmentService.getDownloadableAssignment(assignmentId, loginUser);
        return download(assignment.getFileStoredName(), assignment.getFileOriginalName());
    }

    // 제출 첨부: 제출한 학생 본인 또는 강좌 주인 강사
    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<Resource> downloadSubmissionFile(@AuthenticationPrincipal LoginUser loginUser,
                                                           @PathVariable Long submissionId) {
        Submission submission = submissionService.getDownloadableSubmission(submissionId, loginUser);
        return download(submission.getFileStoredName(), submission.getFileOriginalName());
    }

    private ResponseEntity<Resource> download(String storedName, String originalName) {
        Resource resource = fileStorageService.load(storedName);

        // 다운로드 이름 = 원래 이름 (한글 파일명도 깨지지 않게 UTF-8)
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(originalName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
