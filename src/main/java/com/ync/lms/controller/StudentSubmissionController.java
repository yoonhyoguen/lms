package com.ync.lms.controller;

import com.ync.lms.security.LoginUser;
import com.ync.lms.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StudentSubmissionController {

    private final SubmissionService submissionService;

    // S4 결과 확인: 내 제출 · 점수 · 피드백
    @GetMapping("/student/submissions")
    public String mySubmissions(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("submissions", submissionService.findMySubmissions(loginUser.getId()));
        return "student/submissions";
    }
}
