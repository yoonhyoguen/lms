package com.ync.lms.controller;

import com.ync.lms.domain.Assignment;
import com.ync.lms.security.LoginUser;
import com.ync.lms.service.AssignmentService;
import com.ync.lms.service.FileStorageService;
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
 * 과제 첨부파일 다운로드 (강사, 학생 공통 주소)
 */
@Controller
@RequiredArgsConstructor
public class AssignmentFileController {

    private final AssignmentService assignmentService;
    private final FileStorageService fileStorageService;

    @GetMapping("/assignments/{assignmentId}/file")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal LoginUser loginUser,
                                             @PathVariable Long assignmentId) {
        // 강좌 주인 강사 또는 수강생만 (아니면 403)
        Assignment assignment = assignmentService.getDownloadableAssignment(assignmentId, loginUser);
        Resource resource = fileStorageService.load(assignment.getFileStoredName());

        // 다운로드 이름 = 원래 이름 (한글 파일명도 깨지지 않게 UTF-8)
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(assignment.getFileOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
