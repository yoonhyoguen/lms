package com.ync.lms.controller;

import com.ync.lms.security.LoginUser;
import com.ync.lms.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 강사: 제출 현황(I4), 채점(I5)
 */
@Controller
@RequiredArgsConstructor
public class InstructorSubmissionController {

    private final SubmissionService submissionService;

    // I4 제출 현황 + 미제출자
    @GetMapping("/instructor/assignments/{assignmentId}/submissions")
    public String submissions(@AuthenticationPrincipal LoginUser loginUser,
                              @PathVariable Long assignmentId,
                              Model model) {
        model.addAttribute("view", submissionService.getSubmissionStatus(assignmentId, loginUser.getId())); // B5
        return "instructor/submissions";
    }
}
