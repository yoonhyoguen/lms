package com.ync.lms.controller;

import com.ync.lms.security.LoginUser;
import com.ync.lms.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student/assignments")
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final AssignmentService assignmentService;

    // S2 내 과제 목록 (수강 중인 모든 강좌)
    @GetMapping
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("assignments", assignmentService.findForStudent(loginUser.getId()));
        return "student/assignments";
    }

    // S2 과제 상세 (B2: 수강생만)
    @GetMapping("/{assignmentId}")
    public String detail(@AuthenticationPrincipal LoginUser loginUser,
                         @PathVariable Long assignmentId,
                         Model model) {
        model.addAttribute("assignment", assignmentService.getForStudent(assignmentId, loginUser.getId()));
        return "student/assignment-detail";
    }
}
